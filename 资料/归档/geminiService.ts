
import { GoogleGenAI } from "@google/genai";
import { BatchedResult, DIMENSION_NAMES } from "./types";

const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });

export const analyzeTrends = async (batches: BatchedResult[]): Promise<string> => {
  if (batches.length < 2) return "需要至少两轮数据（10次测试）才能进行趋势分析。";

  const latest = batches[batches.length - 1];
  const previous = batches[batches.length - 2];

  const prompt = `
    请作为一名专业的数据分析师，分析以下两轮测试（每轮5次测试的平均值）的成绩变化：
    
    上一轮（完成日期: ${previous.completionDate}）:
    - ${DIMENSION_NAMES.dimension1}: ${previous.avgScores.dimension1.toFixed(1)}
    - ${DIMENSION_NAMES.dimension2}: ${previous.avgScores.dimension2.toFixed(1)}
    - ${DIMENSION_NAMES.dimension3}: ${previous.avgScores.dimension3.toFixed(1)}
    - ${DIMENSION_NAMES.dimension4}: ${previous.avgScores.dimension4.toFixed(1)}
    - ${DIMENSION_NAMES.dimension5}: ${previous.avgScores.dimension5.toFixed(1)}

    最新一轮（完成日期: ${latest.completionDate}）:
    - ${DIMENSION_NAMES.dimension1}: ${latest.avgScores.dimension1.toFixed(1)}
    - ${DIMENSION_NAMES.dimension2}: ${latest.avgScores.dimension2.toFixed(1)}
    - ${DIMENSION_NAMES.dimension3}: ${latest.avgScores.dimension3.toFixed(1)}
    - ${DIMENSION_NAMES.dimension4}: ${latest.avgScores.dimension4.toFixed(1)}
    - ${DIMENSION_NAMES.dimension5}: ${latest.avgScores.dimension5.toFixed(1)}

    请简要指出：
    1. 进步最明显的维度
    2. 需要注意的退步维度（如果有）
    3. 给用户的下一步建议
    回答请保持简洁专业，使用中文。
  `;

  try {
    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: prompt,
    });
    return response.text || "无法生成分析报告。";
  } catch (error) {
    console.error("Gemini Analysis Error:", error);
    return "分析服务暂时不可用，请稍后再试。";
  }
};
