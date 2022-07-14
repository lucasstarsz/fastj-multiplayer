module fastj.network.test {
    requires fastj.network;
    requires org.junit.jupiter.api;

    opens unittest to org.junit.platform.commons;
}