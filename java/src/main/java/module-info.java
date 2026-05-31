/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * GitHub Copilot SDK for Java.
 */
module com.github.copilot.java {
    requires transitive com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires static com.github.spotbugs.annotations;
    requires static java.compiler;
    requires static java.net.http;
    requires java.logging;

    exports com.github.copilot;
    exports com.github.copilot.generated;
    exports com.github.copilot.generated.rpc;
    exports com.github.copilot.rpc;

    opens com.github.copilot to com.fasterxml.jackson.databind;
    opens com.github.copilot.generated to com.fasterxml.jackson.databind;
    opens com.github.copilot.generated.rpc to com.fasterxml.jackson.databind;
    opens com.github.copilot.rpc to com.fasterxml.jackson.databind;
}
