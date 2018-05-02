package com.intel.cosbench.driver.generator;

import java.util.Random;

import org.apache.commons.lang.StringUtils;

import com.intel.cosbench.config.ConfigException;

public class ScopeStringGenerator implements StringGenerator{
	
	private int min;
    private int max;
    
    public ScopeStringGenerator(int min, int max) {
        if (min <= 0 || max <= 0 || min > max)
            throw new IllegalArgumentException();
        this.min = min;
        this.max = max;
  
    }

	@Override
	public String next(Random random) {
		return min+","+max;
	}

	@Override
	public String next(Random random, int idx, int all) {
		// TODO Auto-generated method stub
		return next(random);
	}
	public static ScopeStringGenerator parse(String pattern) {
        try {
            return tryParse(pattern);
        } catch (Exception e1) {
            if (!StringUtils.startsWith(pattern, "sc("))
                return null;
            try {
                return tryParse(pattern);
            } catch (Exception e2) {
            }
        }
        String msg = "illegal iteration pattern: " + pattern;
        throw new ConfigException(msg);
    }

    private static ScopeStringGenerator tryParse(String pattern) {
        pattern = StringUtils.substringBetween(pattern, "(", ")");
        String[] args = StringUtils.split(pattern, ",");
        int min = Integer.parseInt(args[0]);
        int max = Integer.parseInt(args[1]);
        return new ScopeStringGenerator(min, max);
    }
}
