package com.hua.rpc.util;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLUtil {

    private static File getFile(String fileName){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        /**
         * getResource()方法会去classpath下找这个文件，获取到url resource
         */
        URL url = classLoader.getResource(fileName);

        System.out.println(url.getFile());

        File file = new File(url.getFile());
        return file;
    }

    public static Map<String,Class<?>> praseXML(String fileName){
        Map<String,Class<?>> consumer = new HashMap<String, Class<?>>();
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            Document document = saxBuilder.build(new InputStreamReader(
                    new FileInputStream(getFile(fileName)), "utf-8"));
            Element element = document.getRootElement();
            List<Element> elementList = element.getChildren();
            for (Element one : elementList){
                List<Attribute> attributeList = one.getAttributes();
                String name = "", classPath = "";
                for (Attribute attribute : attributeList){
                    if ("id".equals(attribute.getName()))
                        name = attribute.getValue();
                    if ("class".equals(attribute.getName()))
                        classPath = attribute.getValue();
                }
                if (name != "" && classPath != ""){
                    Class<?> clazz = Class.forName(classPath);
                    consumer.put(name, clazz);
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (JDOMException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            System.out.println("找不到类");
            e.printStackTrace();
        }finally {
            saxBuilder = null;
        }
        return consumer;
    }

}
