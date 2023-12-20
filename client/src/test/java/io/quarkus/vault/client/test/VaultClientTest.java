package io.quarkus.vault.client.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ExtendWith(VaultClientTestExtension.class)
public @interface VaultClientTest {

    @interface EngineMount {
        String engine();

        String path();

        String[] options() default "";
    }

    EngineMount[] value() default {};
}
