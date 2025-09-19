# Usage | JTokkit

[Skip to main content](#__docusaurus_skipToContent_fallback)

![Knuddels Logo](/img/logo.png)
**JTokkit** [Getting Started](/docs/getting-started) [JavaDoc](https://javadoc.io/doc/com.knuddels/jtokkit/latest/index.html)
[Jobs](https://jobs.knuddels.de) [GitHub](https://github.com/knuddelsgmbh/jtokkit)

- [Introduction](/docs/getting-started)
- [Usage](/docs/getting-started/usage)
- [Extending JTokkit](/docs/getting-started/extending)
- [Recipes](/docs/getting-started/recipes/chatml)

## Usage

To use JTokkit, first create a new `EncodingRegistry`:

```java
EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
```

Make sure to keep a reference to the registry, as the creation of the registry is expensive. Creating the registry loads the vocabularies from the classpath. The registry itself handles caching of the loaded encodings. It is thread-safe and can safely be used concurrently by multiple components.

If you do not want to automatically load all vocabularies of all encodings on registry creation, you can use the following lazy loading registry:

```java
EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
```

This encoding registry only loads the vocabularies from encodings that are actually accessed. Vocabularies are only loaded once on first access. As with the default encoding registry, make sure to keep a reference to the registry to make use of the in-built caching of the vocabularies. It is thread-safe and can safely be used concurrently by multiple components.

## Getting an encoding from the registry

You can use the registry to get the encodings you need:

```java
// Get encoding via type-safe enum
Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

// Get encoding via string name
Optional<Encoding> encoding = registry.getEncoding("cl100k_base");

// Get encoding for a specific model via type-safe enum
Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4);

// Get encoding for a specific model via string name
Optional<Encoding> encoding = registry.getEncodingForModel("gpt_4");
```

## Encoding and decoding text

You can use an `Encoding` to encode and decode text:

```java
IntArrayList encoded = encoding.encode("This is a sample sentence.");
// encoded = [2028, 374, 264, 6205, 11914, 13]

String decoded = encoding.decode(encoded);
// decoded = "This is a sample sentence."
```

The encoding is also fully thread-safe and can be used concurrently by multiple components.

> **Note**: The library does not support encoding of special tokens. Special tokens are artificial tokens used to unlock capabilities from a model, such as fill-in-the-middle. If the `Encoding#encode` method encounters a special token in the input text, it will throw an `UnsupportedOperationException`.

If you want to encode special tokens as if they were normal text, you can use `Encoding#encodeOrdinary` instead:

```java
encoding.encode("hello <|endoftext|>"); // raises an UnsupportedOperationException
encoding.encodeOrdinary("hello <|endoftext|>"); // returns [15339, 83739, 8862, 728, 428, 91, 29, 1917]
```

## Counting tokens

If all you want is the amount of tokens the text encodes to, you can use the shorthand method `Encoding#countTokens` or `Encoding#countTokensOrdinary`. These methods are faster than the corresponding `encode` methods.

```java
int tokenCount = encoding.countTokens("This is a sample sentence.");
// tokenCount = 6

int tokenCount = encoding.countTokensOrdinary("hello <|endoftext|>");
// tokenCount = 8
```

## Encoding text with truncation

If you want to only encode up until a specified amount of `maxTokens` and truncate after that amount, you can use `Encoding#encode(String, int)` or `Encoding#encodeOrdinary(String, int)`. These methods will truncate the encoded tokens to the specified length. They will automatically handle Unicode characters that were split in half by the truncation by removing those tokens from the end of the list.

```java
IntArrayList encoded = encoding.encode("This is a sample sentence.", 3);
// encoded = [2028, 374, 264]

String decoded = encoding.decode(encoded);
// decoded = "This is a"

IntArrayList encoded = encoding.encode("I love 🍕", 4);
// encoded = [40, 3021]

String decoded = encoding.decode(encoded);
// decoded = "I love"
```

```markdown
# Counting Tokens for ChatML | JTokkit

[Skip to main content](#__docusaurus_skipToContent_fallback)

![Knuddels Logo](/img/logo.png)
**JTokkit** [Getting Started](/docs/getting-started) [JavaDoc](https://javadoc.io/doc/com.knuddels/jtokkit/latest/index.html)
[Jobs](https://jobs.knuddels.de) [GitHub](https://github.com/knuddelsgmbh/jtokkit)

- [Introduction](/docs/getting-started)
- [Usage](/docs/getting-started/usage)
- [Extending JTokkit](/docs/getting-started/extending)
- [Recipes](/docs/getting-started/recipes/chatml)

  - [Counting Tokens for ChatML](/docs/getting-started/recipes/chatml)

## Counting Tokens for ChatML

If you are using the OpenAI chat models, you need to account for additional tokens that are added to the input text. This recipe shows how to do that. It is based on this [OpenAI Cookbook example](https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb).

```java
private int countMessageTokens(
        EncodingRegistry registry,
        String model,
        List<ChatMessage> messages // consists of role, content and an optional name) {
    Encoding encoding = registry.getEncodingForModel(model).orElseThrow();
    int tokensPerMessage;
    int tokensPerName;){

    if (model.startsWith("gpt-4")) {
        tokensPerMessage = 3;
        tokensPerName = 1;
    } else if (model.startsWith("gpt-3.5-turbo")) {
        tokensPerMessage = 4; // every message follows <|start|>{role/name}\n{content}<|end|>\n
        tokensPerName = -1; // if there's a name, the role is omitted
    } else {
        throw new IllegalArgumentException("Unsupported model: " + model);
    }

    int sum = 0;
    for (final var message : messages) {
        sum += tokensPerMessage;
        sum += encoding.countTokens(message.getContent());
        sum += encoding.countTokens(message.getRole());
        if (message.hasName()) {
            sum += encoding.countTokens(message.getName());
            sum += tokensPerName;
        }
    }
    sum += 3; // every reply is primed with <|start|>assistant<|message|>
    return sum;
}
```


---


Copyright © 2025 Knuddels