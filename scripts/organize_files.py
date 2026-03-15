import os
import re
import shutil
from datetime import datetime

# 路径配置
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_MARKDOWN = os.path.join(BASE_DIR, 'source', 'stu.md')
SOURCE_FILES_DIR = os.path.join(BASE_DIR, 'source', 'sourseFilis')
DEST_UPLOADS_DIR = os.path.join(BASE_DIR, 'backend', 'uploads')

# 跳过的文件关键词（模板、说明等非学生论文文件）
SKIP_KEYWORDS = ['范本', '附件', '模板', '说明', '要求']


def get_student_map(md_file):
    """从 Markdown 表格解析学生映射: {姓名: 学号} 和 {学号: 姓名}"""
    name_to_id = {}
    id_to_name = {}
    with open(md_file, 'r', encoding='utf-8') as f:
        for line in f:
            parts = [p.strip() for p in line.split('|')]
            if len(parts) >= 3 and parts[1].isdigit():
                sid = parts[1]
                name = parts[2]
                name_to_id[name] = sid
                id_to_name[sid] = name
    return name_to_id, id_to_name


def scan_files(source_dir):
    """扫描所有文件"""
    file_list = []
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            file_path = os.path.join(root, file)
            file_list.append((file_path, file))
    return file_list


def extract_date(filename):
    """从文件名提取日期，返回 YYYYMMDD 格式字符串或 None"""
    patterns = [
        # 20260306, 20260315 等纯数字日期
        (r'20\d{6}', 'raw8'),
        # 2026.3.15, 2026-03-15, 2026_01_17
        (r'20\d{2}[._\-年]\d{1,2}[._\-月]\d{1,2}', 'ymd'),
        # 3.15, 2.14 等月.日格式
        (r'(?<!\d)\d{1,2}\.\d{1,2}(?!\d)', 'md'),
    ]

    for pat, fmt in patterns:
        match = re.search(pat, filename)
        if match:
            date_str = match.group()
            try:
                if fmt == 'raw8':
                    y, m, d = int(date_str[:4]), int(date_str[4:6]), int(date_str[6:8])
                    if 1 <= m <= 12 and 1 <= d <= 31:
                        return f"{y}{m:02d}{d:02d}"
                elif fmt == 'ymd':
                    clean = re.sub(r'[._\-年月]', '.', date_str)
                    parts = clean.split('.')
                    y, m, d = int(parts[0]), int(parts[1]), int(parts[2])
                    return f"{y}{m:02d}{d:02d}"
                elif fmt == 'md':
                    parts = date_str.split('.')
                    m, d = int(parts[0]), int(parts[1])
                    # 9-12月 -> 2025 学期前半, 1-8月 -> 2026 学期后半
                    year = 2025 if m >= 9 else 2026
                    return f"{year}{m:02d}{d:02d}"
            except (ValueError, IndexError):
                pass
    return None


def extract_title(filename, student_name, student_id):
    """从文件名提取论文题目"""
    name_no_ext = os.path.splitext(filename)[0]
    title = name_no_ext

    # 移除学号
    title = title.replace(student_id, '')
    # 移除姓名
    title = title.replace(student_name, '')

    # 移除日期相关模式
    title = re.sub(r'20\d{6}', '', title)
    title = re.sub(r'20\d{2}[._\-年]\d{1,2}[._\-月]\d{1,2}[日]?', '', title)
    title = re.sub(r'(?<!\d)\d{1,2}\.\d{1,2}(?!\d)', '', title)

    # 移除括号里的数字 (1), (2) 等版本标记
    title = re.sub(r'\s*[\(（]\d+[\)）]', '', title)

    # 移除首尾无意义字符
    title = title.strip(' _-—，。')

    # 如果以下划线开头，去掉
    title = title.lstrip('_')

    if not title:
        title = "未命名论文"

    return title


def match_student(filename, file_path, name_to_id):
    """从文件名/路径识别学生，返回 (姓名, 学号) 或 (None, None)"""
    # 优先通过姓名匹配（姓名更唯一）
    for name, sid in name_to_id.items():
        if name in filename:
            return name, sid

    # 通过学号匹配
    for name, sid in name_to_id.items():
        if sid in filename:
            return name, sid

    # 通过路径匹配（子目录名可能包含姓名）
    for name, sid in name_to_id.items():
        if name in file_path:
            return name, sid

    return None, None


def main():
    name_to_id, id_to_name = get_student_map(SOURCE_MARKDOWN)
    print(f"加载了 {len(name_to_id)} 名学生信息")

    files = scan_files(SOURCE_FILES_DIR)
    print(f"扫描到 {len(files)} 个文件\n")

    moved = 0
    skipped = []
    unmatched = []

    for file_path, filename in files:
        # 跳过模板/说明文件
        if any(kw in filename for kw in SKIP_KEYWORDS):
            skipped.append(filename)
            print(f"[SKIP] 非论文文件: {filename}")
            continue

        # 识别学生
        student_name, student_id = match_student(filename, file_path, name_to_id)
        if not student_name:
            unmatched.append(filename)
            print(f"[WARN] 无法匹配学生: {filename}")
            continue

        # 提取日期
        date_str = extract_date(filename)
        if not date_str:
            mtime = os.path.getmtime(file_path)
            dt = datetime.fromtimestamp(mtime)
            date_str = dt.strftime("%Y%m%d")
            print(f"  [INFO] 使用文件修改时间作为日期: {date_str}")

        # 提取题目
        title = extract_title(filename, student_name, student_id)

        # 构建新文件名: 姓名学号_题目_日期.ext
        ext = os.path.splitext(filename)[1]
        new_filename = f"{student_name}{student_id}_{title}_{date_str}{ext}"

        # 目标路径
        dest_dir = os.path.join(DEST_UPLOADS_DIR, student_id)
        os.makedirs(dest_dir, exist_ok=True)
        dest_path = os.path.join(dest_dir, new_filename)

        # 冲突处理：自动添加后缀
        base, extension = os.path.splitext(new_filename)
        counter = 1
        while os.path.exists(dest_path):
            new_filename = f"{base}_{counter}{extension}"
            dest_path = os.path.join(dest_dir, new_filename)
            counter += 1

        if counter > 1:
            print(f"  [RENAME] 冲突重命名: {new_filename}")

        print(f"[MOVE] {filename}\n    -> {student_id}/{new_filename}")
        shutil.copy2(file_path, dest_path)
        moved += 1

    # 汇总
    print(f"\n{'='*60}")
    print(f"整理完成！")
    print(f"  已处理: {moved} 个文件")
    print(f"  已跳过: {len(skipped)} 个文件")
    if unmatched:
        print(f"  未匹配: {len(unmatched)} 个文件")
        for f in unmatched:
            print(f"    - {f}")


if __name__ == '__main__':
    main()
