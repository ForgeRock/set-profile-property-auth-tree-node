/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes 
 *
 */

/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.*;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import com.iplanet.sso.SSOException;
import javax.inject.Inject;
import java.util.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetProfilePropertyNode.Config.class)
public class SetProfilePropertyNode extends SingleOutcomeNode {

    private final static String DEBUG_FILE = "SetProfilePropertyNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private final CoreWrapper coreWrapper;


    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         * @return a map of properties.
         */
        @Attribute(order = 100)
        Map<String, String> properties();
    }

    private final Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public SetProfilePropertyNode(@Assisted Config config, CoreWrapper coreWrapper) {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) {

        AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(),context.sharedState.get(REALM).asString());

        Set<String> configKeys = config.properties().keySet();
        for (String key : configKeys) {

            String propertyValue = config.properties().get(key);

            String result = "";
            if (propertyValue.startsWith("\"")) {
                result = propertyValue.substring(1,propertyValue.length()-1);
            } else if (context.sharedState.isDefined(propertyValue)) {
                result = context.sharedState.get(propertyValue).toString().replace("\"","");;
            }
            Map<String, Set> map = new HashMap<String, Set>();
            Set<String> values = new HashSet<String>();
            values.add(result);
            map.put(key, values);
            try {
                userIdentity.setAttributes(map);
                userIdentity.store();
            } catch (IdRepoException e) {
                debug.error("[" + DEBUG_FILE + "]: " + " Error storing profile atttibute '{}' ", e);
            } catch (SSOException e) {
                debug.error("[" + DEBUG_FILE + "]: " + "Node exception", e);
            }
        }

        return goToNext().build();
    }
}
