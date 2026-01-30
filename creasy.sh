# !/bin/bash

export JAVA_FX_HOME=javafx-sdk-25.0.2                                                                                                  
java \
    --module-path "$JAVA_FX_HOME/lib" \
    --add-modules javafx.controls,javafx.fxml \
    -jar target/Creasy-0.1.0.jar