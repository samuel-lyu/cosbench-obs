package com.intel.cosbench.driver.generator;

import java.util.Random;

public class DefaultScopeGenerator implements ScopeGenerator{
	private long base;
    private StringGenerator generator;
    
    
	public DefaultScopeGenerator() {
		
	}
	@Override
	public String next(Random random) {
		// TODO Auto-generated method stub
		return generator.next(random);
	}
	public void setGenerator(StringGenerator generator) {
        this.generator = generator;
    }
}
