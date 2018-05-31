/** 
 
Copyright 2013 Intel Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/ 

package com.intel.cosbench.controller.web;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.bench.Metrics;
import com.intel.cosbench.bench.Snapshot;
import com.intel.cosbench.model.*;

public class TimelinePageController extends StagePageController {

	private String metricName = "throughput";  //The metric shown in the echart chart, the default metric is throughput.
	private String bandWidthUnit = "B/s";  //The unit of bandWidth, the default unit is Byte/s.
	private String byteCountUnit = "B";  //The unit of byteCount, the default unit is Byte.
	private String opCountUnit = "ops";  //The unit of opCount, the default unit is ops.
	
    public TimelinePageController() {
		super();
	}

	public TimelinePageController(String metricName) {
		super();
		this.metricName = metricName;
	}

	@Override
	protected ModelAndView process(HttpServletRequest req, HttpServletResponse res) {
    	if (StringUtils.isNotEmpty(req.getParameter("metricName"))) {
    		this.metricName = req.getParameter("metricName");
		}
		return super.process(req, res);
	}

    @Override
	protected ModelAndView process(WorkloadInfo wInfo, StageInfo sInfo) {
        ModelAndView result = new ModelAndView("timeline");
        Snapshot[] snapshots = sInfo.getSnapshots();
        result.addObject("wInfo", wInfo);
        result.addObject("sInfo", sInfo);
        result.addObject("yAxisName", getYAxisName(metricName, snapshots));
        result.addObject("allMetricsName", getAllMetricsName(snapshots));
        result.addObject("allMetricsData", getAllMetricsData(snapshots));
        return result;
    }
    
    /**
     * According to the number of bandWidth, byteCount, opCount, determine their respective units.
     * @param snapshots
     */
    private void determineTheUnit(Snapshot[] snapshots) {
    	if (snapshots.length == 0) {
			return;
		}
    	double bandWidthSum = 0; //Sum of non zero bandwidth
    	int bandWidthNum = 0; //Number of non zero bandwidth
    	double byteCountSum = 0; //Sum of non zero byteCount
    	int byteCountNum = 0; //Number of non zero byteCount
    	double opCountSum = 0; //Sum of non zero opCount
    	int opCountNum = 0; //Number of non zero opCount
    	for (int i = 0; i < snapshots.length; i++) {
			Metrics[] metrics = snapshots[i].getReport().getAllMetrics();
			for (int j = 0; j < metrics.length; j++) {
				if (metrics[j].getBandwidth() > 0) {
					bandWidthSum += metrics[j].getBandwidth();
					bandWidthNum++;
				}
				if (metrics[j].getByteCount() > 0) {
					byteCountSum += metrics[j].getByteCount();
					byteCountNum++;
				}
				if (metrics[j].getSampleCount() > 0) {
					opCountSum += metrics[j].getSampleCount();
					opCountNum++;
				}
			}
		}
    	
    	if (bandWidthNum > 0) {
			double avgBandWidth = bandWidthSum / bandWidthNum;
			if (avgBandWidth > 1000000000000000.00) {
				bandWidthUnit = "PB/S";
			} else if (avgBandWidth > 1000000000000.00) {
				bandWidthUnit = "TB/S";
			} else if (avgBandWidth > 1000000000.00) {
				bandWidthUnit = "GB/S";
			} else if (avgBandWidth > 1000000.00) {
				bandWidthUnit = "MB/S";
			} else if (avgBandWidth > 1000.00) {
				bandWidthUnit = "KB/S";
			}
		}
    	if (byteCountNum > 0) {
    		double avgByteCount = byteCountSum / byteCountNum;
    		if (avgByteCount > 1000000000000000.00) {
    			byteCountUnit = "PB";
    		} else if (avgByteCount > 1000000000000.00) {
    			byteCountUnit = "TB";
    		} else if (avgByteCount > 1000000000.00) {
    			byteCountUnit = "GB";
    		} else if (avgByteCount > 1000000.00) {
    			byteCountUnit = "MB";
    		} else if (avgByteCount > 1000.00) {
    			byteCountUnit = "KB";
    		}
    	}
    	if (opCountSum > 0) {
    		double avgOpCount = opCountSum / opCountNum;
    		if (avgOpCount > 1000000000.00) {
    			opCountUnit = "gops";
    		} else if (avgOpCount > 1000000.00) {
    			opCountUnit = "mops";
    		} else if (avgOpCount > 1000.00) {
    			opCountUnit = "kops";
    		}
    	}
    }
    
    /**
     * Setting the Y axis name based on the metric name
     * @param metricName
     * @return
     */
    protected String getYAxisName(String metricName, Snapshot[] snapshots) {
    	determineTheUnit(snapshots);
    	String yAxisName = "'Throughput(op/s)'";
    	if (metricName.equals("throughput")) {
    		yAxisName = "'Throughput(op/s)'";
		} else if (metricName.equals("bandWidth")) {
			yAxisName = "'BandWidth(" + bandWidthUnit +")'";
		} else if (metricName.equals("byteCount")) {
			yAxisName = "'ByteCount(" + byteCountUnit +")'";
		} else if (metricName.equals("opCount")) {
			yAxisName = "'OpCount(" + opCountUnit +")'";
		} else if (metricName.equals("avgResTime")) {
			yAxisName = "'AvgResTime(ms)'";
		} else if (metricName.equals("avgProceTime")) {
			yAxisName = "'AvgProceTime(ms)'";
		}
    	return yAxisName;
	}
    
    /**
     * Gets the string consisting of the names of all operations at this stage.
     * @param sInfo
     * @return
     */
    protected String getAllMetricsName(Snapshot[] snapshots) {
    	String metricsName = "[";
    	if (snapshots.length == 0) {
			return "[]";
		}
    	Metrics[] allMetrics = snapshots[0].getReport().getAllMetrics();
    	for (int i = 0; i < allMetrics.length; i++) {
    		metricsName += "'" + allMetrics[i].getOpName() + "',";
		}
    	metricsName = removeTheEndComma(metricsName);
    	metricsName += "]";
    	return metricsName;
    }
    
    /**
     * Gets every operation snapshot collection at this stage and combines it into the form required for ehart diagrams.
     * @param sInfo
     * @return
     */
    protected String getAllMetricsData(Snapshot[] snapshots) {
    	determineTheUnit(snapshots);
		String data = "[";
		List<String> metricsNameList = new ArrayList<String>();
		List<String> metricsDataList = new ArrayList<String>();
		if (snapshots.length == 0) {
			return "[]";
		}
		Metrics[] allMetrics = snapshots[0].getReport().getAllMetrics();
		for (int i = 0; i < allMetrics.length; i++) {
			metricsNameList.add(allMetrics[i].getOpName());
		}
		for (int i = 0; i < snapshots.length; i++) {
			Metrics[] metrics = snapshots[i].getReport().getAllMetrics();
			for (int j = 0; j < metrics.length; j++) {
				DecimalFormat df = new DecimalFormat("#.00");
				if (metricName.equals("throughput")) {
					metricsDataList.add(df.format(metrics[j].getThroughput()) + "");
				} else if (metricName.equals("bandWidth")) {
					double originalBandWidth = metrics[j].getBandwidth();
					String formattedBandWidth;
					if (bandWidthUnit.equals("PB/S")) {
						formattedBandWidth = df.format(originalBandWidth / 1000000000000000.00);
					} else if (bandWidthUnit.equals("TB/S")) {
						formattedBandWidth = df.format(originalBandWidth / 1000000000000.00);
					} else if (bandWidthUnit.equals("GB/S")) {
						formattedBandWidth = df.format(originalBandWidth / 1000000000.00);
					} else if (bandWidthUnit.equals("MB/S")) {
						formattedBandWidth = df.format(originalBandWidth / 1000000.00);
					} else if (bandWidthUnit.equals("KB/S")) {
						formattedBandWidth = df.format(originalBandWidth / 1000.00);
					} else {
						formattedBandWidth = df.format(originalBandWidth);
					}
					metricsDataList.add(formattedBandWidth);
				} else if (metricName.equals("byteCount")) {
					double originalByteCount = metrics[j].getByteCount();
					String formattedByteCount;
					if (byteCountUnit.equals("PB")) {
						formattedByteCount = df.format(originalByteCount / 1000000000000000.00);
					} else if (byteCountUnit.equals("TB")) {
						formattedByteCount = df.format(originalByteCount / 1000000000000.00);
					} else if (byteCountUnit.equals("GB")) {
						formattedByteCount = df.format(originalByteCount / 1000000000.00);
					} else if (byteCountUnit.equals("MB")) {
						formattedByteCount = df.format(originalByteCount / 1000000.00);
					} else if (byteCountUnit.equals("KB")) {
						formattedByteCount = df.format(originalByteCount / 1000.00);
					} else {
						formattedByteCount = df.format(originalByteCount);
					}
					metricsDataList.add(formattedByteCount);
				} else if (metricName.equals("opCount")) {
					double originalOpCount = metrics[j].getSampleCount();
					String formattedOpCount;
					if (opCountUnit.equals("gops")) {
						formattedOpCount = df.format(originalOpCount / 1000000000.00);
					} else if (opCountUnit.equals("mops")) {
						formattedOpCount = df.format(originalOpCount / 1000000.00);
					} else if (opCountUnit.equals("kops")) {
						formattedOpCount = df.format(originalOpCount / 1000.00);
					} else {
						formattedOpCount = df.format(originalOpCount);
					}
					metricsDataList.add(formattedOpCount);
				} else if (metricName.equals("avgResTime")) {
					metricsDataList.add(df.format(metrics[j].getAvgResTime()) + "");
				} else if (metricName.equals("avgProceTime")) {
					metricsDataList.add(df.format((metrics[j].getAvgResTime() - metrics[j].getAvgXferTime())) + "");
				}
			}
		}
		for (int i = 0; i < metricsNameList.size(); i++) {
			data += "{name:'" + metricsNameList.get(i) + "',data:[";
			for (int j = 0; j < metricsDataList.size(); j++) {
				if (j % metricsNameList.size() == i) {
					data += metricsDataList.get(j) + ",";
				}
			}
			data = removeTheEndComma(data);
			if (i == metricsNameList.size() - 1) {
				data += "],type:'line'}";
			} else {
				data += "],type:'line'},";
			}
		}
		data += "]";
    	return data;
	}
    
    private String removeTheEndComma(String originalStr) {
    	String processedStr = originalStr;
		if (StringUtils.isNotEmpty(originalStr) && originalStr.endsWith(",")) {
			processedStr = originalStr.substring(0, originalStr.length() - 1);
		}
		return processedStr;
	}

}
