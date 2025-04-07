package notbe.tmtm.ddanddanserver.config

import notbe.tmtm.ddanddanserver.config.converter.DateToLocalDateConverter
import notbe.tmtm.ddanddanserver.config.converter.DateToLocalDateTimeConverter
import notbe.tmtm.ddanddanserver.config.converter.LocalDateTimeToDateConverter
import notbe.tmtm.ddanddanserver.config.converter.LocalDateToDateConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@Configuration
@EnableMongoAuditing
class MongoConfig {
    @Bean
    fun mappingMongoConverter(
        factory: MongoDatabaseFactory,
        context: MongoMappingContext,
        kstLocalDateTimeWriterConverter: LocalDateTimeToDateConverter,
        kstDateTimeReaderConverter: DateToLocalDateConverter,
        kstLocalDateTimeReaderConverter: DateToLocalDateTimeConverter,
        kstDateTimeWriterConverter: LocalDateToDateConverter,
    ) = MappingMongoConverter(DefaultDbRefResolver(factory), context).apply {
        setTypeMapper(DefaultMongoTypeMapper(null))
        customConversions =
            MongoCustomConversions(
                listOf(
                    kstLocalDateTimeWriterConverter,
                    kstLocalDateTimeReaderConverter,
                    kstDateTimeWriterConverter,
                    kstDateTimeReaderConverter,
                ),
            )
        afterPropertiesSet()
    }
}
