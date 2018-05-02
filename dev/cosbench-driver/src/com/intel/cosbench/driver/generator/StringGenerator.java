package com.intel.cosbench.driver.generator;

import java.util.Random;

public interface StringGenerator {
	
	public String next(Random random);
    public String next(Random random, int idx, int all);
    
}
