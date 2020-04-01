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
/*
 * jon.knight@forgerock.com
 *
 * Needed to register the node
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class SetProfilePropertyNodePlugin extends AbstractNodeAmPlugin {

    @Override
    public String getPluginVersion() {
        return "1.1.1";
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return singletonMap(getPluginVersion(), singleton(SetProfilePropertyNode.class));
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0") || fromVersion.equals("1.0.1") || fromVersion.equals("1.1.0")) {
            pluginTools.upgradeAuthNode(SetProfilePropertyNode.class);
        }
        super.upgrade(fromVersion);
    }
}
