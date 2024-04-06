package net.a.g.excel.rest.test;

import io.quarkus.test.junit.QuarkusTestProfile;

public class Profile2 implements QuarkusTestProfile {

	public Profile2(){}
	
    @Override
    public String getConfigProfile() {
        return "profile2";
    }
}