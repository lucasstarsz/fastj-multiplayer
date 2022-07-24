module fastj.network.test {
    requires fastj.network;
    requires org.slf4j;
    requires org.junit.jupiter.api;

    opens mock to fastj.network, org.junit.platform.commons;

    opens unittest to org.junit.platform.commons;
    opens unittest.serial to org.junit.platform.commons;
    opens unittest.serial.util to org.junit.platform.commons;
    opens unittest.session to org.junit.platform.commons;
}