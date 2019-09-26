/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

package org.red5.server.messaging;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Out-of-band control message used by inter-components communication which are connected with pipes. 
 * Out-of-band data is a separate data stream used for specific purposes (in TCP it's referenced as "urgent data"), like lifecycle control.
 * 与管道相连的组件间通信使用的带外控制消息。带外数据是用于特定目的的单独数据流（在TCP中，它被称为“紧急数据”），如生命周期控制。
 * <tt>'Target'</tt> is used to represent the receiver who may be interested for receiving. 
 * It's a string of any form. XXX shall we design a standard form for Target, like "class.instance"?
 *	“target”用于表示可能对接收感兴趣的接收者。 它是任何形式的字符串。我们要为目标设计一个标准的表单吗，比如“class.instance”？
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
@Setter
@Getter
public class OOBControlMessage implements Serializable {

    private static final long serialVersionUID = -6037348177653934300L;
   
    private String target;

    private String serviceName;

    private Map<String, Object> serviceParamMap;

    private Object result;
}
