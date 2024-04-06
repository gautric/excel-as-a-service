package net.a.g.excel.rest.test;

import io.quarkus.test.junit.QuarkusTestProfile;

public class Profile1 implements QuarkusTestProfile {

	public Profile1(){}
	
    @Override
    public String getConfigProfile() {
        return "profile1";
    }
}