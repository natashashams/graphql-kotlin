package com.expediagroup.graphql.test.integration

import com.expediagroup.graphql.TopLevelObject
import com.expediagroup.graphql.execution.OptionalInput
import com.expediagroup.graphql.testSchemaConfig
import com.expediagroup.graphql.toSchema
import graphql.GraphQL
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class OptionalInputTest {
    private val schema = toSchema(
        queries = listOf(TopLevelObject(OptionalInputQuery())),
        config = testSchemaConfig
    )
    private val graphQL = GraphQL.newGraphQL(schema).build()

    @DisplayName("verify optional arguments can be correctly deserialized")
    @ParameterizedTest(name = "{index} ==> {1}")
    @MethodSource("optionalInputTestArguments")
    fun `verify optional arguments can be correctly deserialized`(query: String, expectedResult: String) {
        val result = graphQL.execute(query)
        val data: Map<String, String>? = result.getData()

        val optionalInputResults = data?.values?.first()
        assertEquals(expectedResult, optionalInputResults)
    }

    companion object {
        @JvmStatic
        fun optionalInputTestArguments(): Stream<Arguments> = Stream.of(
            Arguments.of("{ optionalScalarInput }", "input scalar was not specified"),
            Arguments.of("{ optionalScalarInput(input: null) }", "input scalar value: null"),
            Arguments.of("{ optionalScalarInput(input: \"ABC\") }", "input scalar value: ABC"),
            Arguments.of("{ optionalObjectInput }", "input object was not specified"),
            Arguments.of("{ optionalObjectInput(input: null) }", "input object value: null"),
            Arguments.of("{ optionalObjectInput(input: { id: 1, name: \"ABC\" } )  }", "input object value: SimpleArgument(id=1, name=ABC)"),
            Arguments.of("{ inputWithOptionalScalarValues(input: { required: \"ABC\" }) }", "argument with optional scalar was not specified"),
            Arguments.of("{ inputWithOptionalScalarValues(input: { required: \"ABC\" optional: null }) }", "argument scalar value: null"),
            Arguments.of("{ inputWithOptionalScalarValues(input: { required: \"ABC\" optional: 1 }) }", "argument scalar value: 1"),
            Arguments.of("{ inputWithOptionalValues(input: { required: \"ABC\" }) }", "argument with optional object was not specified"),
            Arguments.of("{ inputWithOptionalValues(input: { required: \"ABC\" optional: null }) }", "argument object value: null"),
            Arguments.of("{ inputWithOptionalValues(input: { required: \"ABC\" optional: { id: 1, name: \"XYZ\" } }) }", "argument object value: SimpleArgument(id=1, name=XYZ)"),
            Arguments.of("{ inputWithNestedOptionalValues(input: {argumentOne: {nestedOptionalInput: \"ABC\"} } )}", "not all deeply nested args defined")

        )
    }
}

data class SimpleArgument(
    val id: Int,
    val name: String
)

data class HasOptionalScalarArguments(
    val req: OptionalInput<HasOptionalArguments>,
    val optional: OptionalInput<Int>
)

data class HasOptionalArguments(
    val required: OptionalInput<String>,
    val optional: OptionalInput<SimpleArgument>
)

data class HasNestedOptionalArguments(
    val argumentOne: OptionalInput<DeeplyNestedArguments>,
    val argumentTwo: OptionalInput<Int>
)

data class DeeplyNestedArguments(
    val nestedOptionalInput: OptionalInput<String>,
    val anotherNestedOptionalInput: OptionalInput<SimpleArgument>
)

class OptionalInputQuery {
    fun optionalScalarInput(input: OptionalInput<String>): String = when (input) {
        is OptionalInput.Undefined -> "input scalar was not specified"
        is OptionalInput.Defined<String> -> "input scalar value: ${input.value}"
    }

    fun optionalObjectInput(input: OptionalInput<SimpleArgument>): String = when (input) {
        is OptionalInput.Undefined -> "input object was not specified"
        is OptionalInput.Defined<SimpleArgument> -> "input object value: ${input.value}"
    }

    fun inputWithOptionalScalarValues(input: HasOptionalScalarArguments): String = when (input.optional) {
        is OptionalInput.Undefined -> "argument with optional scalar was not specified"
        is OptionalInput.Defined<Int> -> "argument scalar value: ${input.optional.value}"
    }

    fun inputWithOptionalValues(input: HasOptionalArguments): String = when (input.optional) {
        is OptionalInput.Undefined -> "argument with optional object was not specified"
        is OptionalInput.Defined<SimpleArgument> -> "argument object value: ${input.optional.value}"
    }
    fun inputWithNestedOptionalValues(input: HasNestedOptionalArguments): String {
        return if (input.argumentOne is OptionalInput.Defined<DeeplyNestedArguments>
            && input.argumentOne.value?.nestedOptionalInput is OptionalInput.Defined
            && input.argumentOne.value?.anotherNestedOptionalInput is OptionalInput.Defined) {
            "all deeply nested args defined"
        } else {
            "not all deeply nested args defined"
        }
    }
}
