package com.intel.cosbench.driver.util;

import java.util.Random;

import com.intel.cosbench.config.Config;
import com.intel.cosbench.driver.generator.Generators;
import com.intel.cosbench.driver.generator.ScopeGenerator;

public class ScopePicker {
	private ScopeGenerator generator;

	public ScopePicker() {
		
	}
	
	public void init(Config config) {
		String scope = config.get("scope");
        generator = Generators.getScopGenerator(scope);
	}
	public String pickScope(Random random) {
		return generator.next(random);
	}
	
}
