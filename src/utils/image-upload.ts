export const MAX_IMAGE_UPLOAD_BYTES = 2 * 1024 * 1024;

const TARGET_IMAGE_BYTES = 350 * 1024;
const MAX_IMAGE_DIMENSION = 1400;

const ALLOWED_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
]);

export async function readImageFileAsDataUrl(file: File): Promise<string> {
  validateImageType(file);

  if (file.type === "image/gif") {
    validateUploadSize(file.size);
    return readFileAsDataUrl(file);
  }

  const originalDataUrl = await readFileAsDataUrl(file);
  if (shouldKeepOriginal(file)) {
    validateUploadSize(file.size);
    return originalDataUrl;
  }

  const compressedDataUrl = await compressImageDataUrl(originalDataUrl, file.type);
  validateUploadSize(dataUrlByteLength(compressedDataUrl));
  return compressedDataUrl;
}

export async function optimizeEmbeddedImageDataUrl(dataUrl: string) {
  if (!isEmbeddedImage(dataUrl)) {
    throw new Error("Only embedded image data URLs can be optimized.");
  }

  const mimeType = extractMimeType(dataUrl);
  if (!mimeType || !ALLOWED_IMAGE_TYPES.has(mimeType)) {
    throw new Error("Unsupported embedded image type.");
  }

  if (mimeType === "image/gif") {
    validateUploadSize(dataUrlByteLength(dataUrl));
    return dataUrl;
  }

  const compressedDataUrl = await compressImageDataUrl(dataUrl, mimeType);
  validateUploadSize(dataUrlByteLength(compressedDataUrl));
  return compressedDataUrl;
}

export function isEmbeddedImage(value: string | null | undefined) {
  return typeof value === "string" && value.startsWith("data:image/");
}

export function estimateEmbeddedImageBytes(dataUrl: string) {
  return dataUrlByteLength(dataUrl);
}

function validateImageType(file: File) {
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    throw new Error("請上傳 JPG、PNG、GIF 或 WebP 圖片。");
  }
}

function validateUploadSize(byteLength: number) {
  if (byteLength > MAX_IMAGE_UPLOAD_BYTES) {
    throw new Error("圖片處理後仍超過 2MB，請換較小的圖片。");
  }
}

function shouldKeepOriginal(file: File) {
  return file.size <= TARGET_IMAGE_BYTES;
}

async function readFileAsDataUrl(file: File): Promise<string> {
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string") {
        resolve(reader.result);
        return;
      }

      reject(new Error("圖片讀取失敗，請重新選擇檔案。"));
    };
    reader.onerror = () => reject(new Error("圖片讀取失敗，請重新選擇檔案。"));
    reader.readAsDataURL(file);
  });
}

async function compressImageDataUrl(dataUrl: string, originalType: string): Promise<string> {
  const image = await loadImage(dataUrl);
  const { width, height } = fitWithinMaxDimension(image.naturalWidth, image.naturalHeight);
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;

  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("瀏覽器不支援圖片壓縮，請改用較小的圖片。");
  }

  context.drawImage(image, 0, 0, width, height);

  const candidateTypes = buildCandidateMimeTypes(originalType);
  const qualitySteps = [0.88, 0.8, 0.72, 0.64, 0.56];

  let bestResult = dataUrl;
  for (const mimeType of candidateTypes) {
    for (const quality of qualitySteps) {
      const candidate = canvas.toDataURL(mimeType, quality);
      if (candidate.length < bestResult.length) {
        bestResult = candidate;
      }

      if (dataUrlByteLength(candidate) <= TARGET_IMAGE_BYTES) {
        return candidate;
      }
    }
  }

  return bestResult;
}

function buildCandidateMimeTypes(originalType: string) {
  const candidateTypes = ["image/webp"];
  if (originalType === "image/jpeg" || originalType === "image/webp") {
    candidateTypes.push("image/jpeg");
  } else {
    candidateTypes.push("image/png");
  }
  return candidateTypes;
}

function extractMimeType(dataUrl: string) {
  const mimeTypeMatch = /^data:(image\/[a-zA-Z0-9.+-]+);base64,/.exec(dataUrl);
  return mimeTypeMatch?.[1]?.toLowerCase() ?? null;
}

function fitWithinMaxDimension(width: number, height: number) {
  if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
    return { width, height };
  }

  const scale = Math.min(MAX_IMAGE_DIMENSION / width, MAX_IMAGE_DIMENSION / height);
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
}

async function loadImage(dataUrl: string): Promise<HTMLImageElement> {
  return await new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("圖片解析失敗，請換一張圖片。"));
    image.src = dataUrl;
  });
}

function dataUrlByteLength(dataUrl: string) {
  const [, encodedPayload = ""] = dataUrl.split(",", 2);
  const paddingLength = encodedPayload.endsWith("==")
    ? 2
    : encodedPayload.endsWith("=")
      ? 1
      : 0;
  return Math.floor((encodedPayload.length * 3) / 4) - paddingLength;
}
