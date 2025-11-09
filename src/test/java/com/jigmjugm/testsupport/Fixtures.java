package com.jigmjugm.testsupport;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FailoverIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.navercorp.fixturemonkey.mockito.plugin.MockitoPlugin;

import java.util.List;

public final class Fixtures {
    private Fixtures() {}
    public static FixtureMonkey monkey() {
        return FixtureMonkey.builder()
                .objectIntrospector(
                        new FailoverIntrospector(
                                List.of(
                                        BuilderArbitraryIntrospector.INSTANCE,
                                        FieldReflectionArbitraryIntrospector.INSTANCE,
                                        ConstructorPropertiesArbitraryIntrospector.INSTANCE
                                ), false
                        )
                )
                .enableLoggingFail(false)
                .plugin(new JakartaValidationPlugin())
                .plugin(new MockitoPlugin())
                .build();
    }
}
