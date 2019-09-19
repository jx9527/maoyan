/*package org.red5.conf;

import java.util.HashSet;
import java.util.Set;

import org.red5.SpringContextHolder;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.CoreHandler;
import org.red5.server.MappingStrategy;
import org.red5.server.Server;
import org.red5.server.api.scope.IScopeSecurityHandler;
import org.red5.server.scope.GlobalScope;
import org.red5.server.scope.ScopeResolver;
import org.red5.server.scope.ScopeSecurityHandler;
import org.red5.server.service.ContextServiceResolver;
import org.red5.server.service.HandlerServiceResolver;
import org.red5.server.service.IServiceResolver;
import org.red5.server.service.ScopeServiceResolver;
import org.red5.server.service.ServiceInvoker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class DefaultContext { 
	
	@Bean(name="global.clientRegistry")
    public ClientRegistry clientRegistry(){
        return new ClientRegistry();
    }
	
	@Bean(name="global.serviceInvoker")
    public ServiceInvoker serviceInvoker(){
		ServiceInvoker inv = new ServiceInvoker();
		Set<IServiceResolver> serviceResolvers = new HashSet<IServiceResolver>();
		serviceResolvers.add(new ScopeServiceResolver());
		serviceResolvers.add(new HandlerServiceResolver());
		serviceResolvers.add(new ContextServiceResolver());
		inv.setServiceResolvers(serviceResolvers);
        return inv;
    } 
	@Bean(name="global.mappingStrategy")
    public MappingStrategy mappingStrategy(){ 
        return new MappingStrategy();
    }
	@Bean(name="global.context")
    public Context context(){ 
        return new Context();
    }
	@Bean(name="global.handler")
    public CoreHandler coreHandler(){ 
        return new CoreHandler();
    }
	@Bean(name="global.scope") 
    public GlobalScope globalScope() throws Exception{
		GlobalScope gsope = new GlobalScope(); 
		gsope.setServer((Server)SpringContextHolder.getApplicationContext().getBean("red5.server"));
		gsope.setContext(context());
		gsope.setHandler(coreHandler());
		gsope.setPersistenceClass("org.red5.server.persistence.FilePersistence");
		Set<IScopeSecurityHandler> securityHandlers = new HashSet<>();
		ScopeSecurityHandler handler = new ScopeSecurityHandler();
		handler.setConnectionAllowed(false);
		securityHandlers.add(handler);
		gsope.setSecurityHandlers(securityHandlers);
		return gsope;
    }
	@Bean(name="red5.scopeResolver")
    public ScopeResolver scopeResolver() throws Exception{
		ScopeResolver resolver = new ScopeResolver();
		resolver.setGlobalScope(globalScope());
        return resolver;
    }
}
*/