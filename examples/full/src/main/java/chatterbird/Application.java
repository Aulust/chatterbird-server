package chatterbird;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class Application {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
    logger.info("Starting application context");
		AbstractApplicationContext ctx = new AnnotationConfigApplicationContext(ApplicationConfig.class);
		ctx.registerShutdownHook();
	}
}
