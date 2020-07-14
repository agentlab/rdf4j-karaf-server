package ru.agentlab.rdf4j.jaxrs;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

//@Component(property = { "osgi.jaxrs.extension=true" })
//@Provider
//@Component(service = CorsFilter.class)
//@HttpWhiteboardFilterPattern("/*")    
public class CorsFilter implements Filter {

    protected static final String ALLOW_ORIGIN = getEnv("ALLOW_ORIGIN", "*");
    protected static final Object[] ALLOW_METHODS = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
            HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.HEAD};
    protected static final String ALLOW_HEADERS = "origin, content-type, accept, authorization, user-agent, cookie";
    protected static final String EXPOSE_HEADERS = "Set-Cookie, Cache-Control, Content-Language, Content-Length, Content-Type, Expires, Last-Modified, Pragma";
    protected static final int MAX_AGE = 42 * 60 * 60;
    
    public CorsFilter() {
        System.out.println("Start CorsFilter");
    }

    /*@Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.add("Access-Control-Allow-Credentials", true);
        headers.add("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        headers.addAll("Access-Control-Allow-Methods", ALLOW_METHODS);
        headers.addAll("Access-Control-Allow-Headers", ALLOW_HEADERS);
        headers.addAll("Access-Control-Expose-Headers", EXPOSE_HEADERS);
        headers.add("Access-Control-Max-Age", MAX_AGE);
    }*/

    private static String getEnv(String key, String def) {
        String value = System.getenv(key);
        return value != null ? value : def;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletrequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletrequest;
        System.out.println("CORSFilter HTTP Request: " + request.getMethod());
 
        // Authorize (allow) all domains to consume the content
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Credentials", "true");
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST, DELETE");
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Expose-Headers", EXPOSE_HEADERS);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Max-Age", new Integer(MAX_AGE).toString());
 
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
 
        // For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS handshake
        if (request.getMethod().equals("OPTIONS")) {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }
        // pass the request along the filter chain
        chain.doFilter(request, servletResponse);        
    }

    @Override
    public void destroy() {
    }
}
