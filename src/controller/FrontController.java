package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import util.*;

public class FrontController extends HttpServlet {
    List<String> controllerList;
    HashMap<String, Mapping> urlMethod;
    Utilities utl;

    @Override
    public void init() throws ServletException {
        controllerList = new ArrayList<>();
        urlMethod = new HashMap<>();
        utl = new Utilities();
        utl.initializeControllers(this, this.controllerList, urlMethod);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException, IllegalArgumentException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet FrontController</h1>");
            out.println("<p>" + request.getRequestURL() + "</p>");
            if (utl.ifMethod(request, this.urlMethod) != null) {
                Mapping mapping = utl.ifMethod(request, this.urlMethod);
                out.println("<p> Classe : " + mapping.getKey() + "</p>");
                out.println("<p> Mehtode: " + mapping.getValue() + "</p>");
                String classe = mapping.getKey();
                String methodeName = mapping.getValue();
                Class<?> clazz = Class.forName(classe);
                Object o = clazz.getDeclaredConstructor().newInstance();
                Method method = clazz.getMethod(methodeName, null);
                Object result = method.invoke(o, null);
                if (result != null) {
                    out.println("<p>Résultat de la méthode : " + result.toString() + "</p>");
                }
            }else{
                out.println("<p> Error 404 : Not found </p>");
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
