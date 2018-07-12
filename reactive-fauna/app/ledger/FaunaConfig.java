package ledger;

import com.typesafe.config.Config;

import javax.inject.Inject;

public class FaunaConfig {

    private String dbName;
    private String endPoint;
    private String secret;
    private String deleteDB;

    @Inject
    public FaunaConfig(Config config) {
        Config faunaConfig = config.getConfig("fauna");

        dbName = faunaConfig.getString("dbName");

        String host  = faunaConfig.getString("host");
        String port  = faunaConfig.getString("port");

        endPoint = "http://" + host + ":" + port;

        secret = faunaConfig.getString("secret");
        deleteDB = faunaConfig.getString("delete_db");

        System.out.println("endPoint = " + endPoint);
    }

    public String getDbName() {
        return dbName;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getSecret() {
        return secret;
    }

    public String getDeleteDB() {
        return deleteDB;
    }

    @Override
    public String toString() {
        return "FaunaConfig{" +
            "dbName='" + dbName + '\'' +
            ", endPoint='" + endPoint + '\'' +
            ", secret='" + secret + '\'' +
            ", deleteDB='" + deleteDB + '\'' +
            '}';
    }
}
