package controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import annotation.AnnotationController;
import annotation.AnnotationMethode;

public class FrontController extends HttpServlet {
    HashMap<String, Map<String, List<String>>> urlMethodMap;

    @Override
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        urlMethodMap = new HashMap<>();
        initializeControllers();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();
        String requestPath = request.getServletPath();

        out.println("chemin: " + requestPath);

        if (urlMethodMap.containsKey(requestPath)) {
            Map<String, List<String>> controllerMethodMap = urlMethodMap.get(requestPath);
            for (Map.Entry<String, List<String>> entry : controllerMethodMap.entrySet()) {
                String controller = entry.getKey();
                List<String> methods = entry.getValue();
                out.println("<p>Controller: " + controller + " ; ");
                out.println("Methods: " + methods + "</p>");
            }
        } else {
            out.println("Il n'y a pas de méthode associée à ce chemin");
        }
    }

    private void initializeControllers() {
        try {
            ServletContext context = getServletContext();
            String packageName = context.getInitParameter("Controller");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(file, packageName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanControllers(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(AnnotationMethode.class)) {
                                AnnotationMethode annotation = method.getAnnotation(AnnotationMethode.class);
                                String url = annotation.url();
                                String methodName = method.getName();

                                if (urlMethodMap.containsKey(url)) {
                                    Map<String, List<String>> controllerMethodMap = urlMethodMap.get(url);
                                    if (controllerMethodMap.containsKey(className)) {
                                        controllerMethodMap.get(className).add(methodName);
                                    } else {
                                        List<String> methodList = new ArrayList<>();
                                        methodList.add(methodName);
                                        controllerMethodMap.put(className, methodList);
                                    }
                                } else {
                                    Map<String, List<String>> controllerMethodMap = new HashMap<>();
                                    List<String> methodList = new ArrayList<>();
                                    methodList.add(methodName);
                                    controllerMethodMap.put(className, methodList);
                                    urlMethodMap.put(url, controllerMethodMap);
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
