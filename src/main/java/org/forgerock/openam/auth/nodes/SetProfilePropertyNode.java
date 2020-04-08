/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes
 *
 */

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetProfilePropertyNode.Config.class)
public class SetProfilePropertyNode extends SingleOutcomeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetProfilePropertyNode.class);
    private final CoreWrapper coreWrapper;
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         *
         * @return a map of properties.
         */
        @Attribute(order = 100)
        Map<String, String> properties();

        /**
         * The mapping of profile attributes to transient state variables.
         *
         * @return The mapping between profile attributes and transient state variables.
         */
        @Attribute(order = 200)
        Map<String, String> transientProperties();

        /**
         * A boolean to note whether attributes should be added or replaced
         *
         * @return a map of properties.
         */
        @Attribute(order = 300)
        default boolean addAttributes() {
            return false;
        }
    }

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     *
     * @param config Node configuration.
     */
    @Inject
    public SetProfilePropertyNode(@Assisted Config config, CoreWrapper coreWrapper) {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        String username = context.sharedState.get(USERNAME).asString();
        String realm = context.sharedState.get(REALM).asString();
        AMIdentity userIdentity = coreWrapper.getIdentity(username, realm);

        Map<String, Set<String>> attributes = createAttributeMap(context, userIdentity);

        try {
            userIdentity.setAttributes(attributes);
            userIdentity.store();
        } catch (IdRepoException | SSOException ex) {
            LOGGER.error("Unable to update user {} in realm {} with attributes {}", username, realm, attributes, ex);
        }

        return goToNext().build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> createAttributeMap(TreeContext context, AMIdentity userIdentity)
            throws NodeProcessException {
        Map<String, Set<String>> attributes = new HashMap<>();
        for (Map.Entry<String, String> entry : config.properties().entrySet()) {
            attributes.putAll(convertToAttributes(context.sharedState, entry));
        }
        for (Map.Entry<String, String> entry : config.transientProperties().entrySet()) {
            attributes.putAll(convertToAttributes(context.transientState, entry));
        }
        Set<String> combinedKeys = Sets.union(config.properties().keySet(), config.transientProperties().keySet());
        if (config.addAttributes() && isNotEmpty(combinedKeys)) {
            Map<String, Set<String>> oldAttributesMap;
            try {
                oldAttributesMap = userIdentity.getAttributes(combinedKeys);
            } catch (IdRepoException | SSOException e) {
                LOGGER.error("Unable to retrieve attributes for keys: {}", combinedKeys, e);
                throw new NodeProcessException("Unable to retrieve attributes for keys");
            }

            for (Map.Entry<String, Set<String>> entry : oldAttributesMap.entrySet()) {
                attributes.merge(entry.getKey(), entry.getValue(), Sets::union);
            }
        }
        return attributes;
    }

    private Map<String, Set<String>> convertToAttributes(JsonValue state, Map.Entry<String, String> entry) {
        String key = entry.getKey();
        String propertyValue = entry.getValue();

        Set<String> result = null;
        if (propertyValue.startsWith("\"")) {
            result = singleton(propertyValue.substring(1, propertyValue.length() - 1));
        } else if (state.isDefined(propertyValue)) {
            JsonValue value = state.get(propertyValue);
            if (value.isList()) {
                result = new HashSet<>(state.get(propertyValue).asList(String.class));
            } else {
                result = singleton(state.get(propertyValue).asString());
            }
        }

        if (isEmpty(result)) {
            return emptyMap();
        } else {
            return singletonMap(key, result);
        }
    }
}
