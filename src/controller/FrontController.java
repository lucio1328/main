package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.CustomerSession;
import util.Mapping;
import util.ModelView;
import util.VerbAction;
import annotation.Controller;
import annotation.FieldAnnotation;
import annotation.ObjectParam;
import annotation.Post;
import annotation.RequestParam;
import annotation.Restapi;
import annotation.Url;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import java.util.ArrayList;
import jakarta.servlet.ServletContext;
import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.util.Enumeration;
import java.util.HashMap;

public class FrontController extends HttpServlet {
    private List<String> controllerList = new ArrayList<>();
    private Map<String, Mapping> urlMappings = new HashMap<>();
    private boolean initialized = false;
    Gson gson = new Gson();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        synchronized (this) {
            if (!initialized) {
                try {
                    initControllers(request);
                    initialized = true;
                } catch (Exception e) {
                    try (PrintWriter out = response.getWriter()) {
                        out.println("Initialization error: " + e.getMessage());
                    }
                    return;
                }
            }
        }

        try (PrintWriter out = response.getWriter()) {
            String requestURL = request.getRequestURL().toString();
            String requestMethod = request.getMethod();
            String baseUrl = getBaseUrl(request);

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Framework</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p><b>URL:</b> " + requestURL + "</p>");
            out.println("<p><b>Method:</b> " + requestMethod + "</p>");
            
            out.println("<p><b>Available Controllers:</b></p>");
            out.println("<ul>");
            for (String controller : controllerList) {
                out.println("<li>" + controller + "</li>");
            }
            out.println("</ul>");

            String mappedURL = requestURL.replace(baseUrl, "");
            if (urlMappings.containsKey(mappedURL)) {
                Mapping map = urlMappings.get(mappedURL);
                out.println("<b>Controller Class:</b> " + map.getKey() + "<br>");
                out.println("<b>Associated Method:</b> " + map.getValue() + "<br>");

                try {
                    Class<?> clazz = Class.forName(map.getKey());
                    Method method = null;
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().equals(map.getValue())) {
                            method = m;
                            break;
                        }
                    }

                    if (method == null) {
                        throw new NoSuchMethodException("Method " + map.getValue() + " not found in class " + map.getKey());
                    }

                    // Tester verb
                    if (VerbAction.testVerbAction(mappedURL, requestMethod) == 0) {
                        response.getWriter().write("La methode HTTP correspond : " + requestMethod);
                    } 
                    else {
                        throw new Exception("La methode Http ne correspond pas a l'annotation de la methode");
                    }

                    try {
                        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                        Field[] fields = clazz.getDeclaredFields();
                        
                        for (Field field : fields) {
                            if (field.getType() == CustomerSession.class) {
                                field.setAccessible(true);
                                String capitalizedFieldName = capitalizeFirstLetter(field.getName());
                                Method setCustomerSessionMethod = clazz.getMethod("set" + capitalizedFieldName, CustomerSession.class);
                                CustomerSession customerSession = new CustomerSession();
                                setCustomerSessionMethod.invoke(controllerInstance, customerSession);
                            }
                        }
                        Object[] methodParams = getMethodParameters(method, request);

                        Object result = method.invoke(controllerInstance, methodParams);

                        for (Object param : methodParams) {
                            if (param instanceof CustomerSession) {
                                synchronizeSession(request.getSession(), (CustomerSession) param);
                                out.println("<p>Session synchronized</p>");
                            }
                        }

                        boolean isRestApi = method.isAnnotationPresent(Restapi.class);

                        if (isRestApi) {
                            response.setContentType("application/json;charset=UTF-8");
                            String jsonResponse;
                            if (result instanceof ModelView) {
                                ModelView modelAndView = (ModelView) result;
                                jsonResponse = gson.toJson(modelAndView.getData());
                                out.println(jsonResponse);
                            } else {
                                jsonResponse = gson.toJson(result);
                                out.println(jsonResponse);
                            }
                        }                         
                        else {
                            if (result instanceof String){
                                out.println(result);
                            }
                            else if (result instanceof ModelView) {
                                ModelView modelAndView = (ModelView) result;
                                for (String key : modelAndView.getData().keySet()) {
                                    request.setAttribute(key, modelAndView.getData().get(key));
                                }
                                request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
                                request.getMethod();
                            }
                        }
                        
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if(cause instanceof Exception){
                            throw (Exception) cause;
                        }
                        else {
                            throw new Exception("Erreur lors de l'invocation : "+cause.getMessage());
                        }
                    }
                        
                } catch (Exception e) {
                    out.println("Error invoking method: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        out.println(element.toString());
                    }
                }
            } else {
                out.println("<p>No associated method found for this URL.</p>");
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }    

    private void initControllers(HttpServletRequest request) throws Exception {
        ServletContext context = getServletContext();
        String packageName = context.getInitParameter("Controller");

        if (packageName == null) {
            throw new Exception("No package-to-scan parameter found in context.");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));
        boolean packageEmpty = true;

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File directory = new File(resource.toURI());
                if (directory.exists() && isDirectoryNotEmpty(directory)) {
                    packageEmpty = false;
                    scanControllers(directory, packageName);
                }
            }
        }

        if (packageEmpty) {
            throw new Exception("The package " + packageName + " is empty.");
        }

        if (controllerList.isEmpty()) {
            throw new Exception("No classes annotated with @Controller found in package " + packageName);
        }
    }

    private boolean isDirectoryNotEmpty(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length > 0;
    }

    private void scanControllers(File directory, String packageName) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Url.class)) {
                                validationMethode(clazz, method);
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception("Class not found: " + className, e);
                }
            }
        }
    }

    private void validationMethode(Class<?> clazz, Method method) throws Exception {
        if (method.getReturnType().equals(String.class) || method.getReturnType().equals(ModelView.class)) {
            String url = null;
            if (method.isAnnotationPresent(Url.class)) {
                Url getAnnotation = method.getAnnotation(Url.class);
                url = getAnnotation.url();
            }

            if (url != null && urlMappings.containsKey(url)) {
                throw new Exception("URL " + url + " is already defined.");
            } else if (url != null) {
                if(method.isAnnotationPresent(Post.class)){
                    urlMappings.put(url, new Mapping(clazz.getName(), method.getName(), new VerbAction(url, "POST")));
                }
                else{
                    urlMappings.put(url, new Mapping(clazz.getName(), method.getName(), new VerbAction(url, "GET")));
                }
            }
        } else {
            throw new Exception("Method return type must be String or ModelAndView.");
        }
    }
    

    private Object[] getMethodParameters(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            ObjectParam objectParam = parameters[i].getAnnotation(ObjectParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value();
                String paramValue = request.getParameter(paramName);
 
                paramValues[i] = convertParameterValue(paramValue, parameters[i].getType());
            } 
            else if(objectParam != null){
                String objectName = objectParam.value();
                Class<?> classe = parameters[i].getType();
                Object o = classe.getDeclaredConstructor().newInstance();
                Map<String, String[]> parametreMap = request.getParameterMap();
                for(Map.Entry<String, String[]> entry : parametreMap.entrySet()){
                    String paramName = entry.getKey();
                    String[] tab = paramName.split("\\.");
                    
                    String nomParam = tab[0];
                    String field = tab[1];
                    if(tab.length < 2) continue;
                    Field[] fields = classe.getDeclaredFields();
                    for(Field f : fields){
                        String fieldValue = null;
                        FieldAnnotation fieldAnnotation = f.getAnnotation(FieldAnnotation.class);
                        if(fieldAnnotation != null && f.getName().equalsIgnoreCase(fieldAnnotation.name())){
                            fieldValue = request.getParameter(paramName);
                        }
                        else if(f.getName().equalsIgnoreCase(field)){
                            fieldValue = request.getParameter(paramName);
                        }

                        if(fieldValue != null){
                            f.setAccessible(true);
                            f.set(o, convertParameterValue(fieldValue, f.getType()));
                        }
                    }
                }
                paramValues[i] = o;
            }
            else if (parameters[i].getType() == CustomerSession.class) {
                HttpSession session = request.getSession();
                CustomerSession customerSession = new CustomerSession();
                Enumeration<String> attributeNames = session.getAttributeNames();
                HashMap<String, Object> valMap = new HashMap<>();
                while (attributeNames.hasMoreElements()) {
                    String attributeName = attributeNames.nextElement();
                    // customerSession.add(attributeName, session.getAttribute(attributeName));
                    valMap.put(attributeName, session.getAttribute(attributeName));
                }
                customerSession.setValues(valMap);
                paramValues[i] = customerSession;
            }

            if(request == null && objectParam == null && parameters[i].getType() != CustomerSession.class){
                throw new Exception("ETU002517, Type de parametre invalide");
            }
        }
        return paramValues;
    }

    private void synchronizeSession(HttpSession httpSession, CustomerSession customerSession) {
        Map<String, Object> values = customerSession.getValues();
        Enumeration<String> httEnumeration = httpSession.getAttributeNames();
        List<String> attributeRemove = new ArrayList<>();

        while (httEnumeration.hasMoreElements()) {
            String attributeName = httEnumeration.nextElement();
            if (!values.containsKey(attributeName)) {
                attributeRemove.add(attributeName);
            }
        }

        for(String attribute : attributeRemove){
            httpSession.removeAttribute(attribute);
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }    

    private Object convertParameterValue(String paramValue, Class<?> targetType) {
        if (paramValue == null) {
            return null;
        }
    
        if (targetType.equals(String.class)) {
            return paramValue;
        } 
        else if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            return Integer.parseInt(paramValue);
        } 
        else if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            return Double.parseDouble(paramValue);
        }
        else if (targetType.equals(Date.class)) {
            return Date.valueOf(paramValue);
        }  
        else if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(paramValue);
        } 
        else if (targetType.equals(long.class) || targetType.equals(Long.class)) {
            return Long.parseLong(paramValue);
        } 
        else if (targetType.equals(float.class) || targetType.equals(Float.class)) {
            return Float.parseFloat(paramValue);
        } 
        else if (targetType.equals(short.class) || targetType.equals(Short.class)) {
            return Short.parseShort(paramValue);
        } 
        else if (targetType.equals(byte.class) || targetType.equals(Byte.class)) {
            return Byte.parseByte(paramValue);
        } 
        else {
            // Ajouter d'autres types selon vos besoins
            throw new IllegalArgumentException("Type de paramètre non pris en charge: " + targetType.getName());
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String requestURI = request.getRequestURI();
        return requestURL.substring(0, requestURL.length() - requestURI.length()) + request.getContextPath() + "/";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            handleException(e, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            handleException(e, response);
        }
    }

    private void handleException(Exception e, HttpServletResponse response) throws IOException {
        e.printStackTrace();
        try (PrintWriter out = response.getWriter()) {
            out.println("Error: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                out.println(element.toString());
            }
        }
    }
}
