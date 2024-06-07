package util;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import annotation.Controller;
import annotation.Get;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Utilities {
    HashMap<String, Mapping> hashMap;

    // Sprint 1 : show the url
    public void initializeControllers(HttpServlet svr, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) throws Exception {
        try {
            ServletContext context = svr.getServletContext();
            String packageName = context.getInitParameter("Controller");

            if (packageName == null || packageName.trim().isEmpty()) {
                throw new Exception("No package controller defined");
            } else if (!this.ifPackageExist(packageName)) {
                throw new Exception("Package '" + packageName + "' not found");
            } else {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if (resource.getProtocol().equals("file")) {
                        File file = new File(resource.toURI());
                        scanControllers(file, packageName, controllerList, urlMethod);
                    }
                }
            }

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    // Sprint 2 : show Controller
    public void scanControllers(File directory, String packageName, List<String> controllerList,
            HashMap<String, Mapping> urlMethod) throws Exception {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName(), controllerList, urlMethod);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllerList.add(className);
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Get.class)) {
                                Get annt = method.getAnnotation(Get.class);
                                Mapping map = new Mapping();
                                map.add(clazz.getName(), method.getName());
                                if (urlMethod.putIfAbsent(annt.value(), map) != null) {
                                    if (!urlMethod.containsKey(annt.value())) {
                                        urlMethod.put(annt.value(), map);
                                    } else {
                                        throw new Exception("url : " + annt.value() + " duplicated");
                                    }
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        this.hashMap = urlMethod;
    }

    public String extractRelativePath(HttpServletRequest request) {
        String fullUrl = request.getRequestURL().toString(); // Obtenez l'URL complet
        String[] relativePath = fullUrl.split("/");// Supprimer le chemin de base de l'URL complet
        return relativePath[relativePath.length - 1];
    }

    public Mapping ifMethod(HttpServletRequest request, HashMap<String, Mapping> urlMethod) {
        String method = this.extractRelativePath(request);
        if (urlMethod.containsKey(method)) {
            return urlMethod.get(method);
        }
        return null;
    }

    // Sprint 3 : call the method of the controller
    public Object callMethod(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        try {
            // get the class
            Class<?> clazz = Class.forName(mapping.getKey());

            // Class method
            Object obj = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod(mapping.getValue().trim());
            return (Object) method.invoke(obj);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    // Sprint 4 : redirect to another page and send all the attribut if the returned
    // value is ModelVIew
    public void MappingHandler(HttpServletRequest request, HttpServletResponse response, Mapping mapping)
            throws Exception {
        Object obj = this.callMethod(request, response, mapping);
        if (obj instanceof ModelView) {
            ModelView mv = (ModelView) obj;
            if (mv.getProperties() != null && !mv.getProperties().isEmpty()) {
                for (Map.Entry<String, Object> entry : mv.getProperties().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key != null && value != null) {
                        request.setAttribute(key, value);
                    } else {
                        throw new Exception("Null key or value found: key = " + key + ", value = " + value);
                    }
                }
                request.getRequestDispatcher(mv.getUrl()).forward(request, response);
            } else {
                throw new Exception("The properties HashMap is null or empty.");
            }

            // Construct the correct relative URL
            String relativeUrl = mv.getUrl();
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(relativeUrl);
            dispatcher.forward(request, response);
        } else if (obj instanceof String) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<p>Classe : " + mapping.getKey() + "</p>");
                out.println("<p>Méthode : " + mapping.getValue() + "</p>");
                out.println("<p>Value returned : " + obj + "</p>");
            }
        } else {
            throw new Exception("the return value an controler methods must be String or ModelView");
        }
    }

    // Sprint 5 : Exception handler
    public void runFramework(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (this.ifMethod(request, this.hashMap) != null) {
            Mapping mapping = this.ifMethod(request, this.hashMap);
            this.MappingHandler(request, response, mapping);
        } else {
            throw new Exception("Il n'y a pas de methode associee a cet url");
        }
    }

    public boolean ifPackageExist(String namePackage) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(namePackage);
        if (resource != null) {
            return true;
        } else {
            return false;
        }
    }
}
