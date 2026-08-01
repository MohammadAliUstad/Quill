import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions/v2";

initializeApp();
setGlobalOptions({ maxInstances: 10 });

export { bookChat } from "./ai/chat/bookChat";
export { generalChat } from "./ai/chat/generalChat";
export { quickChat } from "./ai/chat/quickChat";
export { detectIntent } from "./ai/intent/detectIntent";
export { speakText } from "./audio/speakText";