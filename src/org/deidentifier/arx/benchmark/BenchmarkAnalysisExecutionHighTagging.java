/*
 * Source code of the experiments for the entropy metric
 *      
 * Copyright (C) 2015 Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Function;
import de.linearbits.subframe.graph.Labels;
import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotLinesClustered;
import de.linearbits.subframe.graph.Point3D;
import de.linearbits.subframe.graph.Series;
import de.linearbits.subframe.graph.Series3D;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.render.GnuPlotParams;
import de.linearbits.subframe.render.GnuPlotParams.KeyPos;
import de.linearbits.subframe.render.LaTeX;
import de.linearbits.subframe.render.PlotGroup;

/**
 * Example benchmark
 * @author Fabian Prasser
 */
public class BenchmarkAnalysisExecutionHighTagging {

    /**
     * Main
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, ParseException {
        
        List<PlotGroup> groups = new ArrayList<PlotGroup>();

        Series3D series = new Series3D();
        series.append(getSeries("results/results-high-execution-3.2.csv", "With tagging"));
        series.append(getSeries("results/results-high-execution-3.2-notag.csv", "Without tagging"));

        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("", 
                                         new Labels("Dataset", "Execution time [s]"),
                                         series));
        
        GnuPlotParams params = new GnuPlotParams();
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d; 
        params.logY = false;
        groups.add(new PlotGroup("Comparison of execution times", plots, params, 1.0d));
        LaTeX.plot(groups, "results/high-execution-tagging", true);
        
    }
    
    /**
     * Returns a series
     * @param file
     * @param label
     * @return
     * @throws IOException
     * @throws ParseException 
     */
    private static Series<Point3D> getSeries(String file, String label) throws IOException, ParseException {
        
        CSVFile csvfile = new CSVFile(new File(file));
        
        Series3D series = new Series3D(csvfile,
                                       new Field("Attributes"),
                                       label,
                                       new Field("TimeHeuristic", Analyzer.VALUE));

        series.transform(new Function<Point3D>(){
            @Override
            public Point3D apply(Point3D arg0) {
                return new Point3D(arg0.x,
                                   arg0.y,
                                   String.valueOf(Double.valueOf(arg0.z) / (1000)));
            }
        });
        
        return series;
    }
}
