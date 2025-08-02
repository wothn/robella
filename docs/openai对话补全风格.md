# 对话补全 API

## 请求

**POST** `/chat/completions`

根据输入的上下文，来让模型补全对话内容。

### 请求体 (application/json)

#### messages (object[], required)
对话的消息列表，至少包含1个元素。

消息可以是以下四种类型之一：

##### System message
- **content** (string, required): system 消息的内容。
- **role** (string, required): 该消息的发起角色，其值为 `system`。
- **name** (string, optional): 可以选填的参与者的名称，为模型提供信息以区分相同角色的参与者。

##### User message
- **content** (Text content, required): user 消息的内容。
- **role** (string, required): 该消息的发起角色，其值为 `user`。
- **name** (string, optional): 可以选填的参与者的名称，为模型提供信息以区分相同角色的参与者。

##### Assistant message
- **content** (string or null, required): assistant 消息的内容。
- **role** (string, required): 该消息的发起角色，其值为 `assistant`。
- **name** (string, optional): 可以选填的参与者的名称，为模型提供信息以区分相同角色的参与者。
- **prefix** (boolean, optional): (Beta) 设置此参数为 true，来强制模型在其回答中以此 `assistant` 消息中提供的前缀内容开始。
- **reasoning_content** (string or null, optional): (Beta) 用于推理模型在对话前缀续写功能下，作为最后一条 assistant 思维链内容的输入。使用此功能时，`prefix` 参数必须设置为 `true`。

##### Tool message
- **role** (string, required): 该消息的发起角色，其值为 `tool`。
- **content** (Text content, required): tool 消息的内容。
- **tool_call_id** (string, required): 此消息所响应的 tool call 的 ID。

#### model (string, required)
使用的模型的 ID。

#### frequency_penalty (number, optional)
介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其在已有文本中的出现频率受到相应的惩罚，降低模型重复相同内容的可能性。默认值: 0

#### max_tokens (integer, optional)
介于 1 到 8192 间的整数，限制一次请求中模型生成 completion 的最大 token 数。输入 token 和输出 token 的总长度受模型的上下文长度的限制。如未指定此参数，默认使用 4096。

#### presence_penalty (number, optional)
介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其是否已在已有文本中出现受到相应的惩罚，从而增加模型谈论新主题的可能性。默认值: 0

#### response_format (object, optional)
一个 object，指定模型必须输出的格式。
- **type** (string): 可选值: `text`, `json_object`。默认值: `text`。设置为 { "type": "json_object" } 以启用 JSON 模式，该模式保证模型生成的消息是有效的 JSON。

#### stop (string or string[], optional)
一个 string 或最多包含 16 个 string 的 list，在遇到这些词时，API 将停止生成更多的 token。

#### stream (boolean, optional)
如果设置为 True，将会以 SSE（server-sent events）的形式以流式发送消息增量。消息流以 `data: [DONE]` 结尾。

#### stream_options (object, optional)
流式输出相关选项。只有在 `stream` 参数为 `true` 时，才可设置此参数。
- **include_usage** (boolean): 如果设置为 true，在流式消息最后的 `data: [DONE]` 之前将会传输一个额外的块。此块上的 usage 字段显示整个请求的 token 使用统计信息，而 choices 字段将始终是一个空数组。所有其他块也将包含一个 usage 字段，但其值为 null。

#### temperature (number, optional)
采样温度，介于 0 和 2 之间。更高的值，如 0.8，会使输出更随机，而更低的值，如 0.2，会使其更加集中和确定。我们通常建议可以更改这个值或者更改 `top_p`，但不建议同时对两者进行修改。默认值: 1

#### top_p (number, optional)
作为调节采样温度的替代方案，模型会考虑前 `top_p` 概率的 token 的结果。所以 0.1 就意味着只有包括在最高 10% 概率中的 token 会被考虑。我们通常建议修改这个值或者更改 `temperature`，但不建议同时对两者进行修改。默认值: 1

#### tools (object[], optional)
模型可能会调用的 tool 的列表。目前，仅支持 function 作为工具。使用此参数来提供以 JSON 作为输入参数的 function 列表。最多支持 128 个 function。
- **type** (string, required): tool 的类型。目前仅支持 function。
- **function** (object, required):
  - **description** (string, optional): function 的功能描述，供模型理解何时以及如何调用该 function。
  - **name** (string, required): 要调用的 function 名称。必须由 a-z、A-Z、0-9 字符组成，或包含下划线和连字符，最大长度为 64 个字符。
  - **parameters** (object, optional): function 的输入参数，以 JSON Schema 对象描述。

#### tool_choice (object, optional)
控制模型调用 tool 的行为。
- `none` 意味着模型不会调用任何 tool，而是生成一条消息。
- `auto` 意味着模型可以选择生成一条消息或调用一个或多个 tool。
- `required` 意味着模型必须调用一个或多个 tool。
- 通过 `{"type": "function", "function": {"name": "my_function"}}` 指定特定 tool，会强制模型调用该 tool。
- 当没有 tool 时，默认值为 `none`。如果有 tool 存在，默认值为 `auto`。

#### logprobs (boolean, optional)
是否返回所输出 token 的对数概率。如果为 true，则在 `message` 的 `content` 中返回每个输出 token 的对数概率。

#### top_logprobs (integer, optional)
一个介于 0 到 20 之间的整数 N，指定每个输出位置返回输出概率 top N 的 token，且返回这些 token 的对数概率。指定此参数时，logprobs 必须为 true。

## 响应

### 200 成功响应 (非流式, application/json)

返回一个 `chat completion` 对象。

#### id (string, required)
该对话的唯一标识符。

#### choices (object[], required)
模型生成的 completion 的选择列表。
- **finish_reason** (string, required): 模型停止生成 token 的原因。可能值: `stop`, `length`, `content_filter`, `tool_calls`, `insufficient_system_resource`
  - `stop`：模型自然停止生成，或遇到 `stop` 序列中列出的字符串。
  - `length` ：输出长度达到了模型上下文长度限制，或达到了 `max_tokens` 的限制。
  - `content_filter`：输出内容因触发过滤策略而被过滤。
  - `insufficient_system_resource`：系统推理资源不足，生成被打断。
- **index** (integer, required): 该 completion 在模型生成的 completion 的选择列表中的索引。
- **message** (object, required): 模型生成的 completion 消息。
  - **content** (string or null, required): 该 completion 的内容。
  - **reasoning_content** (string or null, optional): 仅适用于推理模型。内容为 assistant 消息中在最终答案之前的推理内容。
  - **tool_calls** (object[], optional): 模型生成的 tool 调用，例如 function 调用。
    - **id** (string, required): tool 调用的 ID。
    - **type** (string, required): tool 的类型。目前仅支持 `function`。
    - **function** (object, required): 模型调用的 function。
      - **name** (string, required): 模型调用的 function 名。
      - **arguments** (string, required): 要调用的 function 的参数，由模型生成，格式为 JSON。
  - **role** (string, required): 生成这条消息的角色，值为 `assistant`。
- **logprobs** (object, optional): 该 choice 的对数概率信息。
  - **content** (object[], optional): 一个包含输出 token 对数概率信息的列表。
    - **token** (string, required): 输出的 token。
    - **logprob** (number, required): 该 token 的对数概率。`-9999.0` 代表该 token 的输出概率极小，不在 top 20 最可能输出的 token 中。
    - **bytes** (integer[], optional): 一个包含该 token UTF-8 字节表示的整数列表。
    - **top_logprobs** (object[], required): 一个包含在该输出位置上，输出概率 top N 的 token 的列表，以及它们的对数概率。

#### created (integer, required)
创建聊天完成时的 Unix 时间戳（以秒为单位）。

#### model (string, required)
生成该 completion 的模型名。

#### system_fingerprint (string, required)
This fingerprint represents the backend configuration that the model runs with。

#### object (string, required)
对象的类型, 其值为 `chat.completion`。

#### usage (object, required)
该对话补全请求的用量信息。
- **completion_tokens** (integer, required): 模型 completion 产生的 token 数。
- **prompt_tokens** (integer, required): 用户 prompt 所包含的 token 数。该值等于 `prompt_cache_hit_tokens + prompt_cache_miss_tokens`
- **prompt_cache_hit_tokens** (integer, required): 用户 prompt 中，命中上下文缓存的 token 数。
- **prompt_cache_miss_tokens** (integer, required): 用户 prompt 中，未命中上下文缓存的 token 数。
- **total_tokens** (integer, required): 该请求中，所有 token 的数量（prompt + completion）。
- **completion_tokens_details** (object, optional): completion tokens 的详细信息。
  - **reasoning_tokens** (integer, optional): 推理模型所产生的思维链 token 数量

### 200 成功响应 (流式, text/event-stream)

返回一系列 `chat completion chunk` 对象的流式输出。

#### id (string, required)
该对话的唯一标识符。

#### choices (object[], required)
模型生成的 completion 的选择列表。
- **delta** (object, required): 流式返回的一个 completion 增量。
  - **content** (string or null, optional): completion 增量的内容。
  - **reasoning_content** (string or null, optional): 仅适用于 deepseek-reasoner 模型。内容为 assistant 消息中在最终答案之前的推理内容。
  - **role** (string, optional): 产生这条消息的角色，值为 `assistant`。
- **finish_reason** (string, required): 模型停止生成 token 的原因。可能值: `stop`, `length`, `content_filter`, `tool_calls`, `insufficient_system_resource`
- **index** (integer, required): 该 completion 在模型生成的 completion 的选择列表中的索引。

#### created (integer, required)
创建聊天完成时的 Unix 时间戳（以秒为单位）。流式响应的每个 chunk 的时间戳相同。

#### model (string, required)
生成该 completion 的模型名。

#### system_fingerprint (string, required)
This fingerprint represents the backend configuration that the model runs with。

#### object (string, required)
对象的类型, 其值为 `chat.completion.chunk`。