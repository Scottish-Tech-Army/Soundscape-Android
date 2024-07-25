# Licenses
## Assets from the iOS app
The original Soundscape iOS app https://github.com/microsoft/soundscape was
under the MIT License. Some of the files in this project come directly from
that project and so are covered by the same license. Specifically they are:

 * The wav files in the sub-directories of `app/src/main/assets`. These
   are the sounds for the audio beacons.
 * The resources in the sub-directories of `app/src/main/res`. This includes
   strings and translations and various icons.

The license for those file is reproduced here for clarity:

```
MIT License

Copyright (c) Microsoft Corporation.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE
```

## FMOD audio engine
The audio engine used is [FMOD Studio by Firelight Technologies Pty Ltd.](https://www.fmod.com). The code
covered by their license is in `app/src/main/cpp/fmod` as well as the Java binary
in `app/libs/fmod.jar`. The end user license for this code is reproduced here, note
that we do not use the FMOD Studio Authoring Tool, just the FMOD Studio Engine in
section 1.2:
```
                    FMOD END USER LICENCE AGREEMENT
                    ===============================

This End User Licence Agreement (EULA) is a legal agreement between you and
Firelight Technologies Pty Ltd (ACN 099 182 448) (us or we) and governs your use
of FMOD Studio Authoring Tool and FMOD Studio Engine, together the Software.

1. GRANT OF LICENCE

1.1 FMOD Studio Authoring Tool

This EULA grants you the right to use FMOD Studio Authoring Tool for all use,
including Commercial use, subject to the following:

    i. FMOD Studio Authoring Tool is used to create content for use with the
    FMOD Studio Engine only;

    ii. FMOD Studio Authoring Tool is not redistributed in any form.

1.2 FMOD Studio Engine

This EULA grants you the right to use FMOD Studio Engine, for personal
(hobbyist), educational (students and teachers) or Non-Commercial use only,
subject to the following:

    i. FMOD Studio Engine is integrated and redistributed in a software
    application (Product) only;

    ii. FMOD Studio Engine is not distributed as part of a game engine or tool
    set;

    iii. FMOD Studio Engine is not used in any Commercial enterprise or for any
    Commercial production or subcontracting, except for the purposes of
    Evaluation or Development of a Commercial Product;

    iv. Non-Commercial use does not involve any form of monetisation,
    sponsorship or promotion;

    v. Product includes attribution in accordance with Clause 3.

2.OTHER USE

For all Commercial use, and any Non Commercial use not permitted by this
license, a separate license is required. Refer to www.fmod.com/licensing for
information.

3. CREDITS

All Products require an in game credit line which must include the words “FMOD
Studio” and “Firelight Technologies Pty Ltd”. Refer to www.fmod.com/attribution
for examples.

4. INTELLECTUAL PROPERTY RIGHTS

We are and remain at all times the owner of the Software (including all
intellectual property rights in or to the Software). For the avoidance of doubt,
nothing in this EULA may be deemed to grant or assign to you any proprietary or
ownership interest or intellectual property rights in or to the Software other
than the rights licensed pursuant to Clause 1.

You acknowledge and agree that you have no right, title or interest in and to
the intellectual property rights in the Software.

5. SECURITY AND RISK

You are responsible for protecting the Software and any related materials at all
times from unauthorised access, use or damage.

6. WARRANTY AND LIMITATION OF LIABILITY

The Software is provided by us “as is” and, to the maximum extent permitted by
law, any express or implied warranties of any kind, including (but not limited
to) all implied warranties of merchantability and fitness for a particular
purpose are disclaimed.

In no event shall we (and our employees, contractors and subcontractors),
developers and contributors be liable for any direct, special, indirect or
consequential damages whatsoever resulting from loss of data or profits, whether
in an action of contract, negligence or other tortious conduct, arising out of
or in connection with the use or performance of the Software.

7. OGG VORBIS CODEC

FMOD uses the Ogg Vorbis codec © 2002, Xiph.Org Foundation.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    i. Redistributions of source code must retain the above copyright notice,
    the list of conditions and the following disclaimer.

    ii. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other material provided with the distribution.

    iii. Neither the name of the Xiph.org Foundation nor the names of its
    contributors may be used to endorse or promote products derived from this
    software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, 
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT 
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

8. RESONANCE AUDIO SDK

FMOD includes Resonance Audio SDK, licensed under the Apache Licence, Version
2.0 (the Licence); you may not use this file except in compliance with the
License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

8. ANDROID PLATFORM CODE

Copyright (C) 2010 The Android Open Source Project All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

9. AUDIOMOTORS DEMO CONTENT

The audiogaming_audiomotors_demo_engine.agp file is provided for evaluation
purposes only and is not to be redistributed. AudioMotors V2 Pro is required to
create your own engine content. Refer to https://lesound.io for information.
```

## Gradlew

The `gradlew` and `gradlew.bat` files have their own license:

```
Copyright 2015 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## New code
The remaining code which makes up the bulk of this repo is original code and is
licensed under the MIT license:

```
MIT License

Copyright (c) Scottish Tech Army 2024

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE
```

