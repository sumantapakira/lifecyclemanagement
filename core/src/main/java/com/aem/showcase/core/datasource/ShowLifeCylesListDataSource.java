package com.aem.showcase.core.datasource;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

@Component(name = "com.aem.showcase.core.datasource.ShowLifeCylesListDataSource", service = Servlet.class, property = {
		"service.description=Life Cycle model Data Source", "sling.servlet.methods=GET",
		"sling.servlet.resourceTypes=" + ShowLifeCylesListDataSource.SERVLET_RT })

public class ShowLifeCylesListDataSource extends SlingSafeMethodsServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = getLogger(ShowLifeCylesListDataSource.class);

	public static final String SERVLET_RT = "cq/lifecycle/components/datasource/lifecycleListDataSource";
	public static String MODEL_PATH = "/var/lifecycle/models";

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse resp)
			throws ServletException, IOException {
		try {
			ResourceResolver resolver = request.getResourceResolver();
			Session sess = (Session) resolver.adaptTo(Session.class);
			Map<String, String> map = new HashMap<String, String>();
			Node modelHomeNode = sess.getNode(MODEL_PATH);

			NodeIterator models = modelHomeNode.getNodes();

			while (models.hasNext()) {
				Node node = models.nextNode();
				String title = node.getProperty("title").getString();
				map.put(title, node.getPath());
			}

			DataSource ds = new SimpleDataSource(new TransformIterator(map.keySet().iterator(), new Transformer() {
				public Object transform(Object o) {
					String item = (String) o;
					ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());

					vm.put("value", map.get(item));
					vm.put("text", item);

					return new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(),
							"nt:unstructured", vm);
				}
			}));
			request.setAttribute(DataSource.class.getName(), ds);

		} catch (Exception e) {

		}

	}

}
