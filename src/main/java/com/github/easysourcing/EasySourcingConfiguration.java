package com.github.easysourcing;

import com.github.easysourcing.message.commands.annotations.HandleCommand;
import com.github.easysourcing.message.events.annotations.HandleEvent;
import com.github.easysourcing.message.snapshots.annotations.ApplyEvent;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;


@ComponentScan("com.github.easysourcing")
@Configuration
public class EasySourcingConfiguration {

  @Autowired
  private ApplicationContext applicationContext;


  private String getHostPackageName() {
    Map<String, Object> annotatedBeans = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
    return annotatedBeans.isEmpty() ? null : annotatedBeans.values().toArray()[0].getClass().getPackage().getName();
  }


  @Bean
  public Reflections reflections() {
    return new Reflections(getHostPackageName(),
        new TypeAnnotationsScanner(),
        new SubTypesScanner(),
        new MethodAnnotationsScanner(),
        new MethodParameterScanner()
    );
  }


  @Bean
  public ConcurrentMap<String, Set<Method>> commandHandlers(Reflections reflections) {
    return reflections.getMethodsAnnotatedWith(HandleCommand.class)
        .stream()
        .filter(method -> method.getParameterCount() == 1 || method.getParameterCount() == 2)
        .collect(Collectors.groupingByConcurrent(method -> method.getParameters()[0].getType().getName(), toSet()));
  }

  @Bean
  public ConcurrentMap<String, Set<Method>> eventHandlers(Reflections reflections) {
    return reflections.getMethodsAnnotatedWith(HandleEvent.class)
        .stream()
        .filter(method -> method.getParameterCount() == 1)
        .collect(Collectors.groupingByConcurrent(method -> method.getParameters()[0].getType().getName(), toSet()));
  }

  @Bean
  public ConcurrentMap<String, Set<Method>> eventSourcingHandlers(Reflections reflections) {
    return reflections.getMethodsAnnotatedWith(ApplyEvent.class)
        .stream()
        .filter(method -> method.getParameterCount() == 2)
        .filter(method -> method.getReturnType() == method.getParameters()[1].getType())
        .collect(Collectors.groupingByConcurrent(method -> method.getParameters()[0].getType().getName(), toSet()));
  }
}