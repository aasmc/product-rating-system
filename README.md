# Product Rating System

Демо проект, демонстрирующий использованием Wilson Score Confidence Interval - статистической
формулы для оценки доверительного интервала пропорции в биномиальном распределении. Простыми 
словами - формула позволяет рассчитать рейтинг товара на основе пользовательких оценок таким
образом, чтобы товары с б*о*льшим количеством положительных оценок оказывались в рейтинге
выше, чем товары с меньшим количеством аналогичных оценок. Например, представим, что товар 
А имеет 100 оценок "5" и 10 оценок "4", в то время как товар Б имеет 3 оценки "5". Если 
рассчитывать рейтинг по среднему арифметическому, то товар Б окажется выше, однако для 
посетителя сайта такое ранжирование будет не совсем корректно. 

Сама формула выглядит так:
![wilson-score.png](art%2Fwilson-score.png)

Где:
- p -  это оценка пропорции успехов в выборке (например, количество положительных отзывов деленное на общее количество отзывов).
- n - общее количество наблюдений (например, общее количество отзывов).
- z - Z-счет, соответствующий выбранному уровню уверенности. Например, для уровня уверенности 95%, z примерно равно 1.96.

Далее по тексту будет приведен пример использования Wilson Score Confidence Interval для расчета
рейтинга товаров, хранящихся в кластере OpenSearch. Я намеренно выбрал OpenSearch а не ElasticSearch,
так как начиная с 20 июля 2023 года Yandex Cloud [более не предоставляет](https://cloud.yandex.com/en/services/managed-elasticsearch) 
возможности новым пользователям создавать кластеры ElasticSearch, а с 11 апреля 2024 года и 
вовсе прекратит предоставлять услуги по доступу к ElasticSearch. 

## Описание архитектуры
Проект состоит из 2-х независимых микросервисов:
- backend-service
- review-aggregator-service

а также инфрастукрутных компонентов:
- PostgreSQL
- Apache Kafka + Zookeeper
- Confluent Schema Registry
- Kafka Connect
- Debezium Postgres Connector
- Кластер OpenSearch из двух нод

### Backend Service
Предоставляет REST API для:
- регистрации пользователей
- создания товаров
- создания отзывов к товарам
- получения списка пользователей
- получение списка товаров
- получение списка отзывов к товарам
- поучение отсортированного по рейтингу списка товаров

Все сущности: `AppUser`, `Product`, `ProductReview` при создании сохраняются в Postgres.

```sql
CREATE TABLE IF NOT EXISTS app_users(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USERNAME TEXT NOT NULL,
    FIRST_NAME TEXT NOT NULL,
    LAST_NAME TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS products(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME TEXT NOT NULL,
    DESCRIPTION TEXT,
    PRICE NUMERIC(20, 5) NOT NULL
);

CREATE TABLE IF NOT EXISTS product_reviews(
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USER_ID BIGINT NOT NULL,
    PRODUCT_ID BIGINT NOT NULL,
    RATING INTEGER NOT NULL,
    CREATED_AT TIMESTAMP NOT NULL,
    COMMENT TEXT,
    CONSTRAINT product_reviews_app_users_fk FOREIGN KEY (USER_ID) REFERENCES app_users(ID) ON DELETE NO ACTION,
    CONSTRAINT product_reviews_products_fk FOREIGN KEY (PRODUCT_ID) REFERENCES products(ID) ON DELETE NO ACTION
);

CREATE UNIQUE INDEX IF NOT EXISTS product_reviews_user_id_product_id_idx ON product_reviews(USER_ID, PRODUCT_ID);
```

`Product` также сохраняется в OpenSearch. Для простоты это реализовано с помощью Spring Data OpenSearch 
стартера, то есть фактически отправляется запрос в кластер OpenSearch на добавление документа:

```kotlin
@Retryable(
    value = [IOException::class],
    maxAttempts = 5,
    backoff = Backoff(delay = 1000, multiplier = 1.2, random = true)
)
override fun saveToOpenSearch(persistentProduct: Product) {
    val openSearchProduct = OpenSearchProduct()
    openSearchProduct.name = persistentProduct.name
    openSearchProduct.price = persistentProduct.price
    openSearchProduct.description = persistentProduct.description
    openSearchProduct.id = persistentProduct.id.toString()
    openSearchProductDAO.save(openSearchProduct).also {
        log.info("Successfully saved product to OpenSearch. {}", it)
    }
}
```

Таблица `product_reviews`, куда сохраняются данные об отзывах обрабатывается Debezium Postgres Connector
и данные из нее отправляются в топик Kafka. За работу с Kafka отвечает **Review Aggregator Service**.
Подробности работы Debezium Postgres Connector можно почитать в [моей статье](https://habr.com/ru/articles/779620/).

Backend Service получает список отсортированных по рейтингу тваров из кластера OpenSearch, используя
`org.opensearch.data.client.orhlc.OpenSearchRestTemplate`:
```kotlin
    override fun getAllProductsSortedByWilsonScore(): List<ProductResponse> {
        val searchQuery = NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .withSorts(SortBuilders.fieldSort(openSearchProps.wilsonScoreField).order(SortOrder.DESC))
            .build()
        return openSearchRestTemplate.search(searchQuery, OpenSearchProduct::class.java)
            .map { hit ->
                log.info("Converting product from OpenSearch to API response. {}", hit.content)
                mapOpenSearchHitToResponse(hit.content)
            }.toList()
    }
```

Важно обратить внимане на то, как именно хранятся данные о товарах в OpenSearch:
```kotlin
@Document(indexName = "products")
class OpenSearchProduct(
    @Id
    var id: String? = null,
    @Field(type = FieldType.Text, analyzer = "russian")
    var name: String? = null,
    @Field(type = FieldType.Text, analyzer = "russian")
    var description: String? = null,
    @Field(type = FieldType.Double)
    var price: BigDecimal? = null,
    @Field(name = "wilson-score", type = FieldType.Double)
    var wilsonScore: Double = 0.0,
    @Field(type = FieldType.Object)
    var ratings: Map<String, Int> = hashMapOf(
        "1" to 0,
        "2" to 0,
        "3" to 0,
        "4" to 0,
        "5" to 0,
    ),
    @Field(type = FieldType.Long, name = "rating_update_idempotency_key")
    var ratingIdempotencyKey: Long? = null
)
```

Поле `ratings` имеет такю структуру, чтобы была возможность обновлять каждую оценку по отдельности,
поле `wilson-score` рассчитывается скриптом через [scripted updates](https://www.elastic.co/guide/en/elasticsearch/reference/8.11/docs-update.html#_scripted_updates)
при каждом обновлении рейтинга товара, что дает нам возможность ускорить запрос на выборку товаров,
так как поле для сортировки уже подготовлено. Поле `ratingIdempotencyKey` необходимо, чтобы избежать
повторной обработки запросов на обновление рейтинга из-за возможных сбоев в работе Kafka, так как
при взаимодействии с внешними системами Kafka не дает гарантии `exactly_once`, а только `at_least_once`.


### Review Aggregator Service
Собственно говоря, именно этот сервис отвечает за агрегацию данных о пользовательских отзывах
к товарам обновление записей в OpenSearch кластере.

Агрегация осуществляется с помощью Kafka Streams. Далее приведу код с небольшими пояснениями, 
более подробно работа Kafka Streams разобрана в моей статье 
["Микросервисы на основе событий с Kafka Streams и Spring Boot"](https://habr.com/ru/articles/775900/).

```kotlin
@Service
class KafkaReviewAggregatorService(
    private val topicProps: TopicProps,
    private val kafkaProps: KafkaProps
) {

    @Autowired
    fun processStreams(builder: StreamsBuilder) {
        val reviewValueSerde = SpecificAvroSerde<AvroReview>()
        configureSerde(reviewValueSerde, false)
        val reviewKeySerde = Serdes.String()

        val productRatingValueSerde = SpecificAvroSerde<AvroProductRating>()
        configureSerde(productRatingValueSerde, false)
        val productRatingKeySerde = Serdes.String()

        val productReviews: KStream<String, AvroReview> = builder.stream(
            topicProps.reviewTopic,
            Consumed.with(reviewKeySerde, reviewValueSerde)
        )

        val aggregatedStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(topicProps.aggregatedReviewsStore),
            Serdes.String(),
            productRatingValueSerde
        )

        builder.addStateStore(aggregatedStore)

        productReviews
            .peek { key, value ->
                log.info("Processing AvroReview: {}. Message Key: {}", value, key)
            }
            .mapValues(::toAvroProductRating)
            .process(
                ProcessorSupplier {
                    AvroReviewPunctuator(topicProps)
                },
                topicProps.aggregatedReviewsStore
            )
            .peek { key, value ->
                log.info("Processing AvroProductRating: {}. Message Key: {}", value, key)
            }
            .to(
                topicProps.aggregatedReviewsTopic,
                Produced.with(productRatingKeySerde, productRatingValueSerde)
            )
    }


    private fun configureSerde(serde: Serde<*>, isKey: Boolean) {
        if (serde is SpecificAvroSerde) {
            val config = hashMapOf<String, Any>(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaProps.schemaRegistryUrl
            )
            serde.configure(config, isKey)
        }
    }


    private fun toAvroProductRating(record: AvroReview): AvroProductRating =
        AvroProductRating(
            record.productId,
            hashMapOf(
                convertDigitToString(record.rating) to 1
            ),
            record.`lsn$1`
        )

    private fun convertDigitToString(digit: Int): String {
        return when(digit) {
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            else -> throw RuntimeException("Unsupported Rating: $digit")
        }
    }
}
```

Тут все достаточно просто:
1. Создаем KStream на основе топика, куда отравляются данные об отзывах
```kotlin
     val productReviews: KStream<String, AvroReview> = builder.stream(
            topicProps.reviewTopic,
            Consumed.with(reviewKeySerde, reviewValueSerde)
        )
```

2. Создаем KeyValue хранилище, которое будет использоваться в агрегации данных:
```kotlin
        val aggregatedStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(topicProps.aggregatedReviewsStore),
            Serdes.String(),
            productRatingValueSerde
        )

        builder.addStateStore(aggregatedStore)
```

3. Мапим значения в стриме в удобную для агрегации сущность:
```kotlin
        productReviews
            .peek { key, value ->
                log.info("Processing AvroReview: {}. Message Key: {}", value, key)
            }
            .mapValues(::toAvroProductRating)
```
Она генерируется с помощью Gradle плагина для генерации Avro классов на основе .avsc файлов.
```json
{
  "type": "record",
  "name": "AvroProductRating",
  "namespace": "ru.aasmc.avro",
  "fields": [
    {
      "name": "productId",
      "type": "long"
    },
    {
      "name": "ratings",
      "type": {
        "type": "map",
        "values": "int"
      }
    },
    {
      "name": "idempotencyKey",
      "type": [
        "null",
        "long"
      ],
      "default": null
    }
  ]
}
```
4. Агрегируем данные в кастомном процесоре (более подробно будет дальше по тексту):
```kotlin
.process(
    ProcessorSupplier {
        AvroReviewPunctuator(topicProps) },
    topicProps.aggregatedReviewsStore
)
```
5. Отправляем полученные данные в новый топик Kafka:
```kotlin
.to(
    topicProps.aggregatedReviewsTopic,
    Produced.with(productRatingKeySerde, productRatingValueSerde)
)
```

При работе с Kafka Streams можно использовать механизмы агрегации данных предоставляемый
фреймворком, однако тут есть один несколько ньюансов:
1. По умолчанию в результирующий топик будут попадать в том числе и промежуточные данные, а в нашем
    случае это увеличит нагрузку на кластер OpenSearch, даже если мы позаботимся о дедуплицировании.
2. При [использовании оператора](https://developer.confluent.io/patterns/stream-processing/suppressed-event-aggregator/) `suppress`, 
    который говорит Kafka Streams отправлять только конечный
    результат аггрегации в результирующий топик, результат будет отправлен при закрытии окна агрегации,
    а оно закрывается при создании нового окна после поступления новых записей. Это потенциально
    может стать проблемой. Например, по нашему сценарию мы хотим обновлять рейтинг товара в 
    кластере OpenSearch каждые 30 минут. Соответственно выставляем окно агрегации в 30 минут. 
    В течение этого времени товару поставили 10 отзывов. После этого в течение суток не было ни
    одной оценки, а через 24 часа поставили еще 5 оценок. При использовании оператора
    `suppress` данные в результирующий топик попадут только через 24 часа, а не через 30 минут,
    как мы этого хотели, из-за того, что окно агрегации закроется при открытии нового окна. Чтобы
    решить эту проблему, приходится писать свою логику агрегации и кастомный процесор с помощью
    [Processor API](https://kafka.apache.org/10/documentation/streams/developer-guide/processor-api.html).

Кастомный процессор выглядит так:
```kotlin
class AvroReviewPunctuator(
    private val topicProps: TopicProps
): Processor<String, AvroProductRating, String, AvroProductRating> {

    private lateinit var context: ProcessorContext<String, AvroProductRating>
    private lateinit var store: KeyValueStore<String, AvroProductRating>

    override fun init(context: ProcessorContext<String, AvroProductRating>) {
        this.context = context
        this.store = context.getStateStore(topicProps.aggregatedReviewsStore) as KeyValueStore<String, AvroProductRating>
        context.schedule(topicProps.punctuationInterval, PunctuationType.WALL_CLOCK_TIME, this::punctuate)
    }


    private fun punctuate(to: Long) {
        log.debug("Enter punctuate method.")
        store.all().forEachRemaining { entry ->
            log.info("Sending new aggregated value from punctuate: {}", entry.value)
            context.forward(Record(entry.key, entry.value, to))
            store.delete(entry.key)
        }
    }

    override fun process(record: Record<String, AvroProductRating>) {
        val key = record.key()
        val newRating = record.value()
        log.debug("Processing record with value: {}", newRating)
        val currentAggregatedRating = store.get(key)
        if (currentAggregatedRating != null) {
            val currentRatings = currentAggregatedRating.ratings
            newRating.ratings.forEach { (ratingKey, ratingValue) ->
                currentRatings.merge(ratingKey, ratingValue, Int::plus)
            }
            val idempotencyKey = maxOf(newRating.idempotencyKey, currentAggregatedRating.idempotencyKey)
            store.put(key, AvroProductRating(newRating.productId, currentRatings, idempotencyKey))
        } else {
            store.put(key, newRating)
        }
    }
}
```

1. Метод init отрабатывает при создании процессора. Тут мы инициализируем ранее подготовленное
    и добавленное в топологию KeyValue хранилище:
```kotlin
this.store = context.getStateStore(topicProps.aggregatedReviewsStore) as KeyValueStore<String, AvroProductRating>
```
2. Настраиваем планировщик, который будет отрабатывать 1 раз за указанный интервал времени `topicProps.punctuationInterval`,
    который мы храним в конфигурации.
```kotlin
context.schedule(topicProps.punctuationInterval, PunctuationType.WALL_CLOCK_TIME, this::punctuate)
```
3. В методе `punctuate`, происходит логика отправки данных в процессор ниже по стриму, и очистке
    хранящихся в KeyValue хранилище данных:
```kotlin
    private fun punctuate(to: Long) {
        log.debug("Enter punctuate method.")
        store.all().forEachRemaining { entry ->
            log.info("Sending new aggregated value from punctuate: {}", entry.value)
            context.forward(Record(entry.key, entry.value, to))
            store.delete(entry.key)
        }
    }
```
4. Метод `process(record: Record<String, AvroProductRating>)` отрабатывает на каждую запись
    из стрима выше. Тут мы получаем данные из KeyValue хранилища, производим слияние рейтингов
    и обновляем запись в хранилище. Стоит обратить внимание, что в качестве idempotencyKey используется
    LSN (Log Sequence Number) отправляемый Debezium Postgres Connector в Kafka при обработке
    таблицы `product_reviews`. Так как LSN - монотонно увеличивающееся значение, в агрегируемой 
    сущности мы проставляем максимальное значение из старой и текущей записи. В дальнейшем LSN будет 
    использован при обновлении документа в кластере OpenSearch. 
```kotlin
    override fun process(record: Record<String, AvroProductRating>) {
        val key = record.key()
        val newRating = record.value()
        log.debug("Processing record with value: {}", newRating)
        val currentAggregatedRating = store.get(key)
        if (currentAggregatedRating != null) {
            val currentRatings = currentAggregatedRating.ratings
            newRating.ratings.forEach { (ratingKey, ratingValue) ->
                currentRatings.merge(ratingKey, ratingValue, Int::plus)
            }
            val idempotencyKey = maxOf(newRating.idempotencyKey, currentAggregatedRating.idempotencyKey)
            store.put(key, AvroProductRating(newRating.productId, currentRatings, idempotencyKey))
        } else {
            store.put(key, newRating)
        }
    }
```

### Обновление данных в OpenSearch

Kafka Consumer вычитывает записи из топика с агрегированными данными и обновляет рейтинг
товара в OpenSearch с помощью painless скрипта:
```kotlin
private const val SCRIPT_SOURCE = """
    if (ctx._source['rating_update_idempotency_key'] == null ||
     ctx._source['rating_update_idempotency_key'] < params.idempotency_key) {
        if (params.containsKey('one')) {
            ctx._source.ratings['1'] = (ctx._source.ratings['1'] ?: 0) + params.one;
        }
        if (params.containsKey('two')) {
            ctx._source.ratings['2'] = (ctx._source.ratings['2'] ?: 0) + params.two;
        }
        if (params.containsKey('three')) {
            ctx._source.ratings['3'] = (ctx._source.ratings['3'] ?: 0) + params.three;
        }
        if (params.containsKey('four')) {
            ctx._source.ratings['4'] = (ctx._source.ratings['4'] ?: 0) + params.four;
        }
        if (params.containsKey('five')) {
            ctx._source.ratings['5'] = (ctx._source.ratings['5'] ?: 0) + params.five;
        }
        long s1 = ctx._source.ratings.containsKey('1') ? ctx._source.ratings['1'] : 0;
        long s2 = ctx._source.ratings.containsKey('2') ? ctx._source.ratings['2'] : 0;
        long s3 = ctx._source.ratings.containsKey('3') ? ctx._source.ratings['3'] : 0;
        long s4 = ctx._source.ratings.containsKey('4') ? ctx._source.ratings['4'] : 0;
        long s5 = ctx._source.ratings.containsKey('5') ? ctx._source.ratings['5'] : 0;
        double p = (s1 * 0.0) + (s2 * 0.25) + (s3 * 0.5) + (s4 * 0.75) + (s5 * 1.0);
        double n = (s1 * 1.0) + (s2 * 0.75) + (s3 * 0.5) + (s4 * 0.25) + (s5 * 0.0);
        double wilsonScore = p + n > 0 ? ((p + 1.9208) / (p + n) - 1.96 * Math.sqrt((p * n) / (p + n) + 0.9604) / (p + n)) / (1 + 3.8416 / (p + n)) : 0;
        ctx._source['wilson-score'] = wilsonScore;
        ctx._source['rating_update_idempotency_key'] = params.idempotency_key;
    }
"""

private const val IDEMPOTENCY_KEY = "idempotency_key"

@Service
class OpenSearchKafkaConsumer(
    private val openSearchRestTemplate: OpenSearchRestTemplate,
    private val props: OpenSearchProps
) {

    @KafkaListener(topics = ["\${topicprops.aggregatedReviewsTopic}"], concurrency = "3")
    fun consumeAndSend(record: AvroProductRating) {
        updateProductInOpenSearch(record)
        log.info("Successfully updated rating of product with id = {}", record.productId)
    }

    private fun updateProductInOpenSearch(record: AvroProductRating) {
        val params = mutableMapOf<String, Any>()
        record.ratings.forEach { (key, value) ->
            params[key] = value
        }
        params[IDEMPOTENCY_KEY] = record.idempotencyKey
        val updateQuery = UpdateQuery.builder(record.productId.toString())
            .withScriptType(ScriptType.INLINE)
            .withLang("painless")
            .withScript(SCRIPT_SOURCE)
            .withParams(params)
            .build()
        openSearchRestTemplate.update(updateQuery, IndexCoordinates.of(props.productIndex))
    }
}
```

1. `ctx._source` позволяет получить доступ к полям обновляемого документа.
2. `params` - параметры, передаваемые в скрипт
3. `if (ctx._source['rating_update_idempotency_key'] == null ||
        ctx._source['rating_update_idempotency_key'] < params.idempotency_key)` 
    Обновление производим только в том случае, если `rating_update_idempotency_key` == null, это
    значит что обновляем рейтинг впервые, или же lsn товара, который используется в качестве ключа
    идемпотентности, меньше того, что пришел в параметрах скрипта, то есть мы обновляем рейтинг
    на основе новых оценок пользователей, а не тех, что были отправлены Kafka Consumer повторно
    из-за того, что он по какой-то причине не закомитил оффсет и перечитал записи.
4. Далее обновляем значения в каждой оценки в зависимости от того, что пришло в параметрах. 
5. `double p = (s1 * 0.0) + (s2 * 0.25) + (s3 * 0.5) + (s4 * 0.75) + (s5 * 1.0);` Рассчитываем
    положительный рейтинг и нормализуем его от 0 до 1, отдавая предпочтение более высоким оценкам
6. `double n = (s1 * 1.0) + (s2 * 0.75) + (s3 * 0.5) + (s4 * 0.25) + (s5 * 0.0);` Рассчитываем
    отрицательный рейтинг и нормализуем его от 0 до 1, отдавая предпочтение более низким оценкам.
7. `double wilsonScore = p + n > 0 ? ((p + 1.9208) / (p + n) - 1.96 * Math.sqrt((p * n) / (p + n) + 0.9604) / (p + n)) / (1 + 3.8416 / (p + n)) : 0;`
    Рассчитываем wilson score по упрощенной формуле. В качестве Z используем 1.96, что
    соответствует 95% уровню уверенности для двустороннего теста. 
8. Сохраняем полученный результат в поле `wilson-score`, а также обновляем ключ идемпотентности.

Модифицированную формулу для расчета Wilson Score Confidence Interval, а также подход
с применением painless скрипта взял из статьи: [Better than Average: Sort by Best Rating with Elasticsearch.](https://www.elastic.co/blog/better-than-average-sort-by-best-rating-with-elasticsearch)

### Проверка работоспособности:
Для того, чтобы проверить работоспособность подхода я подготовил Postman коллекцию,
в которой создается несколько пользователей и товаров, а затем пользователи ставят оценки товарам:
- Товар 1. "1" - 3 оценки, "2" - 2 оценки
- Товар 2. "2" - 3 оценки, "3" - 2 оценки
- Товар 3. "3" - 2 оценки, "4" - 2 оценки, "5" - 1 оценка
- Товар 4. "4" - 3 оценки, "5" - 2 оценки
- Товар 5. "4" - 1 оценка, "5" - 4 оценки
- Товар 6. "4" - 1 оценка, "5" - 1 оценка
- Товар 7. "4" - 1 оценка
- Товар 8. "3" - 1 оценка
- Товар 9. "5" - 1 оценка
- Товар 10. не имеет оценок.

В результате запроса на получение отсортированного по рейтингу списка товаров получаем:
```json
[
  {
    "id": 5,
    "name": "Product Five",
    "price": 14.0,
    "description": "Description Five",
    "wilsonScore": 0.5118538596631975,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 1,
      "5": 4
    }
  },
  {
    "id": 4,
    "name": "Product Four",
    "price": 13.0,
    "description": "Description Four",
    "wilsonScore": 0.41770741047904003,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 3,
      "5": 2
    }
  },
  {
    "id": 3,
    "name": "Product Three",
    "price": 12.0,
    "description": "Description Three",
    "wilsonScore": 0.2987857322245606,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 2,
      "4": 2,
      "5": 1
    }
  },
  {
    "id": 6,
    "name": "Product Six",
    "price": 15.0,
    "description": "Description Six",
    "wilsonScore": 0.26404786368925837,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 1,
      "5": 1
    }
  },
  {
    "id": 9,
    "name": "Product Nine",
    "price": 18.0,
    "description": "Description Nine",
    "wilsonScore": 0.20654329147389294,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 0,
      "5": 1
    }
  },
  {
    "id": 7,
    "name": "Product Seven",
    "price": 16.0,
    "description": "Description Seven",
    "wilsonScore": 0.11790609179425604,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 1,
      "5": 0
    }
  },
  {
    "id": 2,
    "name": "Product Two",
    "price": 11.0,
    "description": "Description Two",
    "wilsonScore": 0.09409051165901611,
    "ratings": {
      "1": 0,
      "2": 3,
      "3": 2,
      "4": 0,
      "5": 0
    }
  },
  {
    "id": 8,
    "name": "Product Eight",
    "price": 17.0,
    "description": "Description Eight",
    "wilsonScore": 0.0546190651458835,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 1,
      "4": 0,
      "5": 0
    }
  },
  {
    "id": 1,
    "name": "Product One",
    "price": 10.0,
    "description": "Description One",
    "wilsonScore": 0.010529638390925532,
    "ratings": {
      "1": 3,
      "2": 2,
      "3": 0,
      "4": 0,
      "5": 0
    }
  },
  {
    "id": 10,
    "name": "Product Ten",
    "price": 19.0,
    "description": "Description Ten",
    "wilsonScore": 0.0,
    "ratings": {
      "1": 0,
      "2": 0,
      "3": 0,
      "4": 0,
      "5": 0
    }
  }
]
```

Как видно, товары с б*о*льшим количеством положительных оценок находятся вверху списка,
а товары вообще без оценок - в самом низу списка. 

## Стек технологий
1. Kotlin
2. Spring Boot 3.2.0
3. Apache Kafka
4. Kafka Connect
5. Debezium Postgres Connector
6. Kafka Streams
7. PostgreSQL
8. OpenSearch
9. Confluent Schema Registry
10. Docker

### Как запустить микросервисы
1. Поднимаем инфраструктурные контейнеры:
```shell
cd docker
docker-compose up -d
```
2. Ждем, когда поднимутся все контейнеры. Дольше всех закускается Kafka Connect.
3. Регистрируем Avro схему агрегированных данных о рейтингах в Confluent Schema Registry
    с помощью Gradle плагина `com.github.imflog.kafka-schema-registry-gradle-plugin`. Для 
    этого в корневой директории проекта необходимо выполнить команду:
```shell
./gradlew registerSchemaTask
```
4. Делаем чистый билд проекта, чтобы сгенерировались Avro классы на основе схем, которые
    лежат в папке `review-aggregator-service/src/main/avro`. Для этого в корневой директории
    проекта необходимо выполнить команду:
```shell
./gradlew clean build -x test
```

5. Отправляем конфигурацию Debezium Postgres Connector в Kafka Connect.
```shell
cd connector
curl -s -S -XPOST -H Accept:application/json -H Content-Type:application/json http://localhost:8083/connectors/ -d @debezium-config.json
```
6. После этого можно стартовать сами микросервисы `backend-service` и `review-aggregator-service`.
    При запуске `backend-service` необходимо добавить JVM опцию `--add-opens java.base/java.math=ALL-UNNAMED`.
    Пока не выяснил, по какой причине Spring Data OpenSearch выбрасывает ошибку при получении
    данных из кластера, если в модели данных на стороне клиента используется тип BigDecimal.
```text
java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.math.BigInteger java.math.BigDecimal.intVal accessible: module java.base does not "opens java.math" to unnamed module @71e9ddb4
```

После этого можно эскпериментировать с эндпоинтами. В качестве отправной точки, можно воспользоваться
коллекцией Postman, которая также есть в директории postman. 


