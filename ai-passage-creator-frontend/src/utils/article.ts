/**
 * 文章相关工具函数
 */
import { STATUS_TEXT_MAP, STATUS_TAG_COLOR_MAP, STATUS_COLOR_MAP } from '@/constants/article'
import { marked } from 'marked'

/**
 * 获取状态文本
 * @param status 状态值
 */
export const getStatusText = (status: string): string => {
  return STATUS_TEXT_MAP[status] || status
}

/**
 * 获取状态标签颜色（用于 Ant Design Tag）
 * @param status 状态值
 */
export const getStatusTagColor = (status: string): string => {
  return STATUS_TAG_COLOR_MAP[status] || 'default'
}

/**
 * 获取状态颜色（用于自定义样式）
 * @param status 状态值
 */
export const getStatusColor = (status: string): string => {
  return STATUS_COLOR_MAP[status] || '#999'
}

/**
 * 导出文章为 Markdown 文件
 * @param title 文章标题
 * @param subTitle 副标题
 * @param content 正文内容
 * @param fullContent 完整图文内容（可选）
 * @param outline 大纲（可选）
 * @param images 配图列表（可选）
 */
export interface ExportArticleOptions {
  title: string
  subTitle?: string
  content?: string
  fullContent?: string
  outline?: Array<{ section: number; title: string }>
  images?: Array<{ description: string; url: string }>
}

export const exportAsMarkdown = (options: ExportArticleOptions): void => {
  const { title, subTitle, content, fullContent, outline, images } = options

  let markdown = `# ${title}\n\n`
  if (subTitle) {
    markdown += `> ${subTitle}\n\n`
  }

  // 优先使用完整图文
  if (fullContent) {
    markdown += fullContent
  } else {
    if (outline && outline.length > 0) {
      markdown += `## 目录\n\n`
      outline.forEach((item) => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }

    markdown += content || ''

    if (images && images.length > 0) {
      markdown += `\n\n## 配图\n\n`
      images.forEach((image) => {
        markdown += `![${image.description}](${image.url})\n\n`
      })
    }
  }

  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title || '文章'}.md`
  a.click()
  URL.revokeObjectURL(url)
}

/**
 * 导出文章为 PDF 文件（直接下载）
 * @param options 导出选项
 */
export const exportAsPdf = async (options: ExportArticleOptions): Promise<void> => {
  const { title, subTitle, content, fullContent } = options

  const [{ default: jsPDF }, { default: html2canvas }] = await Promise.all([
    import('jspdf'),
    import('html2canvas'),
  ])

  const markdownContent = fullContent || content || ''
  const htmlBody = marked(markdownContent)

  // 构建临时容器渲染 HTML
  const container = document.createElement('div')
  container.style.cssText = 'position:fixed;left:-9999px;top:0;width:800px;padding:40px;background:#fff;font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;line-height:1.8;color:#333;'
  container.innerHTML = `
    <h1 style="font-size:24px;text-align:center;margin-bottom:4px;">${title}</h1>
    ${subTitle ? `<p style="font-size:14px;color:#888;text-align:center;margin-top:0;margin-bottom:32px;">${subTitle}</p>` : ''}
    ${htmlBody}
  `
  document.body.appendChild(container)

  try {
    const canvas = await html2canvas(container, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    })

    const imgData = canvas.toDataURL('image/jpeg', 0.95)
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()
    const imgWidth = pageWidth
    const imgHeight = (canvas.height * imgWidth) / canvas.width

    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight

    while (heightLeft > 0) {
      position = -(pageHeight * (Math.ceil(imgHeight / pageHeight) - Math.ceil(heightLeft / pageHeight)))
      pdf.addPage()
      pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight
    }

    pdf.save(`${title || '文章'}.pdf`)
  } finally {
    document.body.removeChild(container)
  }
}
