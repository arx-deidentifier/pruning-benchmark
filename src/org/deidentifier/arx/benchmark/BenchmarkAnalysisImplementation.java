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
import java.util.Iterator;

import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Function;
import de.linearbits.subframe.graph.Point3D;
import de.linearbits.subframe.graph.Series;
import de.linearbits.subframe.graph.Series3D;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.io.CSVLine;

/**
 * Example benchmark
 * @author Fabian Prasser
 */
public class BenchmarkAnalysisImplementation {

    /**
     * Main
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args) throws IOException, ParseException {
        
        CSVFile fileListWithoutAntichain = new CSVFile(new File("results/results-low-execution-3.2-list-noantichain.csv"));
        CSVFile fileListWithoutAntichainOpt = new CSVFile(new File("results/results-low-execution-3.2-list-noantichain-opt.csv"));
        CSVFile fileList = new CSVFile(new File("results/results-low-execution-3.2-list.csv"));
        CSVFile fileListOpt = new CSVFile(new File("results/results-low-execution-3.2-list-opt.csv"));
        CSVFile fileWithoutMin = new CSVFile(new File("results/results-low-execution-3.2-nomin.csv"));
        CSVFile fileWithMin = new CSVFile(new File("results/results-low-execution-3.2.csv"));
        
//        System.out.println("List without antichain");
//        print(fileListWithoutAntichain);
        System.out.println("List without antichain (optimized)");
        print(fileListWithoutAntichainOpt);
//        System.out.println("List");
//        print(fileList);
        System.out.println("List (optimized)");
        print(fileListOpt);
        System.out.println("Trie without minimum");
        print(fileWithoutMin);
        System.out.println("Trie");
        print(fileWithMin);
        
    }
    
    private static void print(CSVFile file) {
        Iterator<CSVLine> iter = file.iterator();
        while (iter.hasNext()) {
            CSVLine line = iter.next();
            String dataset = line.get("", "Dataset");
            double total = Double.valueOf(line.get("TimeOptimal", "Value"));
            double check = Double.valueOf(line.get("TimeCheck", "Value"));
            double lattice = total - check;
            System.out.println(" - " + dataset+": " + lattice);
        }
    }

    /**
     * Returns a series
     * @param file
     * @param label
     * @return
     * @throws IOException
     */
    private static Series<Point3D> getSeries(String file, final String label) throws IOException {
        
        Series3D series = new Series3D(new CSVFile(new File(file)),  
                                       new Field("Dataset"),
                                       label,
                                       new Field("TimeOptimal", Analyzer.VALUE));
        
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
