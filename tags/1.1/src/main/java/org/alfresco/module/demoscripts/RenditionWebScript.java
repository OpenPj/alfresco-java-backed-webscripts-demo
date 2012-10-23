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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class RenditionWebScript extends AbstractWebScript {
	static final String NAMESPACE = "http://www.alfresco.org/model/demoscripts/1.0";

	static final QName TYPE_RENDITION = QName.createQName(NAMESPACE, "rendition");
	static final QName PROP_NOTES = QName.createQName(NAMESPACE, "notes");
	static final QName PROP_TIMESTAMP = QName.createQName(NAMESPACE, "timestamp");

	static final QName ASPECT_RENDITIONABLE = QName.createQName(NAMESPACE, "renditionable");
	static final QName PROP_ASSOC_RENDITIONS = QName.createQName(NAMESPACE, "assocRenditions");

	private ServiceRegistry registry;
	private Repository repository;

	// for Spring injection
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	// for Spring injection
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}

	public void execute(WebScriptRequest req, WebScriptResponse res)
			throws IOException {
		String targetMimetype = req.getParameter("mimetype");
		if (targetMimetype == null || "".equals(targetMimetype)) {
			targetMimetype = "application/x-shockwave-flash";
		}

		// get the referenced incoming node
		NodeRef nodeRef = getNodeRef(req);

		// check to see if it has the "demo:renditionable" aspect
		if (getNodeService().hasAspect(nodeRef, ASPECT_RENDITIONABLE)) {
			// check to see if it has a rendition for this mimetype
			List<AssociationRef> list = getNodeService().getTargetAssocs(
					nodeRef, PROP_ASSOC_RENDITIONS);
			for (AssociationRef x : list) {
				NodeRef childNodeRef = x.getTargetRef();
				if (targetMimetype.equals(guessMimetype(childNodeRef))) {
					// stream back
					output(res, childNodeRef);
					return;
				}
			}
		} else {
			// add the aspect
			getNodeService().addAspect(nodeRef, ASPECT_RENDITIONABLE, null);
		}

		// now generate the rendition
		String sourceMimetype = guessMimetype(nodeRef);

		// try to locate a transformer that will convert from this mimetype to
		// our intended type
		ContentTransformer transformer = getContentService().getTransformer(
				sourceMimetype, targetMimetype);

		// if we don't have a transformer, throw an error
		if (transformer == null) {
			throw new WebScriptException("Unable to locate transformer");
		}

		// determine properties about the new node
		NodeRef parentRef = getNodeService().getPrimaryParent(nodeRef)
				.getParentRef();
		String newNodeName = getFilename(nodeRef) + "."
				+ getMimetypeService().getExtension(targetMimetype);

		// create the new node
		NodeRef newNodeRef = getNodeService().createNode(parentRef,
				ContentModel.ASSOC_CONTAINS,
				QName.createQName(NAMESPACE, newNodeName), TYPE_RENDITION)
				.getChildRef();

		// set the name of the node
		getNodeService().setProperty(newNodeRef, ContentModel.PROP_NAME,
				newNodeName);

		// add the titled aspect
		Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
		aspectProperties.put(ContentModel.PROP_TITLE, newNodeName);
		getNodeService().addAspect(newNodeRef, ContentModel.ASPECT_TITLED,
				aspectProperties);

		// set up transformation options
		TransformationOptions options = new TransformationOptions();
		options.setSourceContentProperty(ContentModel.PROP_CONTENT);
		options.setSourceNodeRef(nodeRef);
		options.setTargetContentProperty(ContentModel.PROP_CONTENT);
		options.setTargetNodeRef(newNodeRef);

		// establish a content reader (from source)
		ContentReader contentReader = getContentReader(nodeRef);
		contentReader.setMimetype(sourceMimetype);

		// establish a content writer (to destination)
		ContentWriter contentWriter = getContentWriter(newNodeRef);
		contentWriter.setMimetype(targetMimetype);

		// do the transformation
		transformer.transform(contentReader, contentWriter, options);

		// set up the association so that we don't do this more than once
		// it is remembered on the object and looked up next time
		getNodeService().createAssociation(nodeRef, newNodeRef,
				PROP_ASSOC_RENDITIONS);

		// stream the result back
		output(res, newNodeRef);
	}

	protected String guessMimetype(NodeRef nodeRef) {
		String filename = getFilename(nodeRef);
		return getMimetypeService().guessMimetype(filename);
	}

	protected String getFilename(NodeRef nodeRef) {
		return getFileFolderService().getFileInfo(nodeRef).getName();
	}

	protected NodeRef getNodeRef(WebScriptRequest req) {
		// NOTE: This web script must be executed in a HTTP Servlet environment
		if (!(req instanceof WebScriptServletRequest)) {
			throw new WebScriptException(
					"Content retrieval must be executed in HTTP Servlet environment");
		}
		HttpServletRequest httpReq = ((WebScriptServletRequest) req)
				.getHttpServletRequest();

		// locate the root path
		String path = httpReq.getParameter("path");
		if (path == null) {
			path = "/images";
		}
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		// build a path elements list
		List<String> pathElements = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(path, "/");
		while (tokenizer.hasMoreTokens()) {
			String childName = tokenizer.nextToken();
			pathElements.add(childName);
		}

		// look up the child
		NodeRef nodeRef = null;
		try {
			NodeRef companyHomeRef = repository.getCompanyHome();
			nodeRef = registry.getFileFolderService()
					.resolveNamePath(companyHomeRef, pathElements).getNodeRef();
		} catch (Exception ex) {
			throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND,
					"Unable to locate path");
		}
		if (nodeRef == null) {
			throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND,
					"Unable to locate path");
		}

		return nodeRef;
	}

	protected void output(WebScriptResponse res, NodeRef nodeRef) {
		// stream back
		try {
			ContentReader reader = registry.getContentService().getReader(
					nodeRef, ContentModel.PROP_CONTENT);
			reader.getContent(res.getOutputStream());
		} catch (Exception ex) {
			throw new WebScriptException("Unable to stream output");
		}
	}

	private ContentService getContentService() {
		return this.registry.getContentService();
	}

	private NodeService getNodeService() {
		return this.registry.getNodeService();
	}

	private MimetypeService getMimetypeService() {
		return this.registry.getMimetypeService();
	}

	private FileFolderService getFileFolderService() {
		return this.registry.getFileFolderService();
	}

	private ContentReader getContentReader(NodeRef nodeRef) {
		return this.registry.getContentService().getReader(nodeRef,
				ContentModel.PROP_CONTENT);
	}

	private ContentWriter getContentWriter(NodeRef nodeRef) {
		return this.registry.getContentService().getWriter(nodeRef,
				ContentModel.PROP_CONTENT, true);
	}

}