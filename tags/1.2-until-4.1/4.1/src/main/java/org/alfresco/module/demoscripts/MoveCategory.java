package org.alfresco.module.demoscripts;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * This webscript shows how to move an Alfresco category. This implementation is
 * taken from the Jeff Potts blog ecmarchitect: {@link http://ecmarchitect.com/archives/2012/03/07/1565}
 * 
 * @author Jeff Potts
 * 
 */
public class MoveCategory extends DeclarativeWebScript {

	// Dependencies
	private NodeService nodeService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status) {

		final String sourceNodeRefString = req.getParameter("sourceNodeRef");
		final String targetNodeRefString = req.getParameter("targetNodeRef");

		// snag the nodes
		NodeRef sourceNodeRef = new NodeRef(sourceNodeRefString);
		String sourceName = (String) nodeService.getProperty(sourceNodeRef,
				ContentModel.PROP_NAME);
		
		NodeRef targetNodeRef = new NodeRef(targetNodeRefString);
		String targetName = (String) nodeService.getProperty(targetNodeRef,
				ContentModel.PROP_NAME);

		// move the source node to the target
		nodeService.moveNode(sourceNodeRef, targetNodeRef,
				ContentModel.ASSOC_SUBCATEGORIES, QName.createQName(
						NamespaceService.CONTENT_MODEL_1_0_URI, sourceName));

		// set up the model
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("sourceNodeRef", sourceNodeRefString);
		model.put("sourceName", sourceName);
		model.put("targetNodeRef", targetNodeRefString);
		model.put("targetName", targetName);

		return model;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}