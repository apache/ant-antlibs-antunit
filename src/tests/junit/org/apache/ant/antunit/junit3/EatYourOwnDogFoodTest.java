package org.apache.ant.antunit.junit3;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.ant.antunit.junit4.AntUnitSuiteRunner;
import org.junit.runner.RunWith;

/**
 * A unit test using the junit3 and junit4 adapter.
 */
@RunWith(AntUnitSuiteRunner.class)
public class EatYourOwnDogFoodTest extends TestCase {

    public static TestSuite suite() {
        File script = new File("src/etc/testcases/antunit/java-io.xml");
        return new AntUnitSuite(script, EatYourOwnDogFoodTest.class);
    }

}
