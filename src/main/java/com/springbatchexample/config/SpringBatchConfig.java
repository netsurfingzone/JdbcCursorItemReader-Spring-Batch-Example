package com.springbatchexample.config;

import com.springbatchexample.component.StudentItemProcessor;
import com.springbatchexample.component.StudentResultRowMapper;
import com.springbatchexample.entity.Student;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.sql.DataSource;

@EnableBatchProcessing
@Configuration
public class SpringBatchConfig {


    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;


    @Bean
    public JdbcCursorItemReader<Student> reader() {
        JdbcCursorItemReader<Student> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("select id, roll_number, name from student");
        reader.setRowMapper(new StudentResultRowMapper());
        reader.setMaxRows(10);
        reader.setFetchSize(10);
        reader.setQueryTimeout(10000);
        return reader;
    }

    @Bean
    public FlatFileItemWriter<Student> writer() {
        FlatFileItemWriter<Student> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("C://data/batch/data.csv"));
        writer.setLineAggregator(getDelimitedLineAggregator());
        return writer;
    }

    private DelimitedLineAggregator<Student> getDelimitedLineAggregator() {
        BeanWrapperFieldExtractor<Student> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<Student>();
        beanWrapperFieldExtractor.setNames(new String[]{"id", "rollNumber", "name"});

        DelimitedLineAggregator<Student> aggregator = new DelimitedLineAggregator<Student>();
        aggregator.setDelimiter(",");
        aggregator.setFieldExtractor(beanWrapperFieldExtractor);
        return aggregator;

    }

    @Bean
    public Step getDbToCsvStep() {
        StepBuilder stepBuilder = stepBuilderFactory.get("getDbToCsvStep");
        SimpleStepBuilder<Student, Student> simpleStepBuilder = stepBuilder.chunk(1);
        return simpleStepBuilder.reader(reader()).processor(processor()).writer(writer()).build();
    }

    @Bean
    public Job dbToCsvJob() {
        JobBuilder jobBuilder = jobBuilderFactory.get("dbToCsvJob");
        jobBuilder.incrementer(new RunIdIncrementer());
        FlowJobBuilder flowJobBuilder = jobBuilder.flow(getDbToCsvStep()).end();
        Job job = flowJobBuilder.build();
        return job;
    }

    @Bean
    public StudentItemProcessor processor() {
        return new StudentItemProcessor();
    }

}
