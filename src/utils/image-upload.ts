export const MAX_IMAGE_UPLOAD_BYTES = 2 * 1024 * 1024;

const ALLOWED_IMAGE_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
]);

export async function readImageFileAsDataUrl(file: File): Promise<string> {
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    throw new Error("僅支援 JPG、PNG、GIF、WebP 圖片。");
  }

  if (file.size > MAX_IMAGE_UPLOAD_BYTES) {
    throw new Error("圖片大小不能超過 2MB。");
  }

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

export function isEmbeddedImage(value: string | null | undefined) {
  return typeof value === "string" && value.startsWith("data:image/");
}
