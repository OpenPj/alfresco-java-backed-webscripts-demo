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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class RandomSelectWebScript extends AbstractWebScript {
	private static Random gen = new Random();
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
		// get the referenced incoming node
		NodeRef rootNodeRef = getNodeRef(req);

		// draw a random child
		NodeRef childNodeRef = randomChild(rootNodeRef);

		// stream child back
		output(res, childNodeRef);
	}

	protected NodeRef randomChild(NodeRef rootNodeRef) {
		// count the number of children
		List<FileInfo> files = registry.getFileFolderService().listFiles(
				rootNodeRef);
		int fileCount = files.size();

		// draw random number
		int draw = gen.nextInt(fileCount);

		// our draw
		FileInfo fileInfo = files.get(draw);
		NodeRef nodeRef = fileInfo.getNodeRef();

		return nodeRef;
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
}