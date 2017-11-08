package com.hua.rpc.spring;

import com.hua.rpc.service.HahaService;
import com.hua.rpc.service.HelloService;
import com.hua.rpc.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 实现自己实例化bean并注册到Spring管理
 */
@Configuration
//@ImportResource("classpath:server-spring.xml")
public class MyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor{
    private static final Logger LOGGER = LoggerFactory.getLogger(MyBeanDefinitionRegistryPostProcessor.class);

    // bean 名称生成器
    private BeanNameGenerator beanNameGenerator =  new AnnotationBeanNameGenerator();

    /**
     * 先执行：postProcessBeanDefinitionRegistry()方法
     * 再执行：postProcessBeanFactory()方法
     * @param beanDefinitionRegistry
     * @throws BeansException
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        LOGGER.info("MyBeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()");
        //生成远程方法代理
        Map<String, Class<?>> consumer = XMLUtil.praseXML("consumer.xml");
        for (String name : consumer.keySet()){
            Class<?> clazz = consumer.get(name);
            Class<?>[] intefaceClass =  clazz.getInterfaces();
            registerBean2(beanDefinitionRegistry, name, intefaceClass[0]);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        LOGGER.info("MyBeanDefinitionRegistryPostProcessor.postProcessBeanFactory()");
    }

    private void registerBean(BeanDefinitionRegistry registry, String name, Class<?> beanClass){
        AnnotatedBeanDefinition annotatedBeanDefinition = new AnnotatedGenericBeanDefinition(beanClass);

        //可以自动生成name
        String beanName = (name == null? name: beanNameGenerator.generateBeanName(annotatedBeanDefinition, registry));
        // bean 注册的hold类
        BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(annotatedBeanDefinition, beanName);

        BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);

    }

    private void registerBean2(BeanDefinitionRegistry registry, String name, Class<?> interfaceClass){
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(ServiceFactoryBean.class);
        beanDefinition.setLazyInit(false);
        beanDefinition.getPropertyValues().add("interfaceClass", interfaceClass);
        String beanName = this.beanNameGenerator.generateBeanName(beanDefinition, registry);
        if (name == null || name == ""){
            registry.registerBeanDefinition(beanName, beanDefinition);
        }else {
            registry.registerBeanDefinition(name, beanDefinition);
        }
    }
}
