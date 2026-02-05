import os
import re
import shutil
from datetime import datetime

# 路径配置
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_MARKDOWN = os.path.join(BASE_DIR, 'source', 'stu.md')
SOURCE_FILES_DIR = os.path.join(BASE_DIR, 'source', 'sourseFilis')
DEST_UPLOADS_DIR = os.path.join(BASE_DIR, 'backend', 'uploads')

def get_student_map(md_file):
    """从 Markdown 表格解析学生映射: {姓名: 学号} 和 {学号: 姓名}"""
    name_to_id = {}
    id_to_name = {}
    with open(md_file, 'r', encoding='utf-8') as f:
        for line in f:
            parts = [p.strip() for p in line.split('|')]
            if len(parts) >= 3 and parts[1].isdigit():
                # parts[1] 是学号, parts[2] 是姓名
                sid = parts[1]
                name = parts[2]
                name_to_id[name] = sid
                id_to_name[sid] = name
    return name_to_id, id_to_name

def scan_files(source_dir):
    """扫描所有文件，生成文件列表 (path, filename)"""
    file_list = []
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            file_path = os.path.join(root, file)
            file_list.append((file_path, file))
    return file_list

def extract_date(filename):
    """尝试从文件名提取日期，返回 formatted date string 或 None"""
    # 匹配模式: 2026.1.17, 1.12, 12.24, 2026-01-01
    patterns = [
        (r'20\d{2}[._-]\d{1,2}[._-]\d{1,2}', '%Y.%m.%d'), # 2026.1.17
        (r'\d{1,2}[._-]\d{1,2}[._-]\d{1,2}', '%y.%m.%d'), # 25.1.17 (rare but possible)
        (r'\d{1,2}[._-]\d{1,2}', '%m.%d')                 # 1.12, 12.24
    ]
    
    for pat, fmt in patterns:
        match = re.search(pat, filename)
        if match:
            date_str = match.group()
            # 规范化分隔符
            date_str_clean = date_str.replace('_', '.').replace('-', '.')
            try:
                # 简单补全
                if fmt == '%m.%d':
                    # 假设是最近的年份，跨年需要注意，这里简单假设是当前学期 2025/2026
                    # 简单的逻辑：如果是 9-12月 -> 2025, 1-8月 -> 2026 (根据当前时间调整)
                    m, d = map(int, date_str_clean.split('.'))
                    year = 2025 if m >= 9 else 2026
                    return f"{year}{m:02d}{d:02d}"
                elif fmt == '%Y.%m.%d':
                    y, m, d = map(int, date_str_clean.split('.'))
                    return f"{y}{m:02d}{d:02d}"
            except:
                pass
    return None

def clean_filename_parts(filename, name, date_str):
    """从文件名中移除姓名和日期，剩下的作为题目"""
    name_no_ext = os.path.splitext(filename)[0]
    
    # 移除姓名
    temp = name_no_ext.replace(name, '')
    
    # 移除日期 (如果有)
    # 这比较难精确匹配，简单的方法是移除数字和特殊字符的组合，或者保留剩下既然是的
    if date_str:
        # 重建简单的日期 regex 去移除
        pass 
        
    # 简单的清洗策略：
    # 1. 移除姓名
    # 2. 移除纯数字开头或结尾
    # 3. 移除特殊字符开头
    
    title = temp
    # 移除可能的学号 (如果文件名里有学号)
    title = re.sub(r'\d{10,}', '', title)
    # 移除日期格式
    title = re.sub(r'\d{1,4}[._]\d{1,2}[._]\d{1,2}', '', title)
    title = re.sub(r'\d{1,2}[._]\d{1,2}', '', title)
    
    # 清理剩下的一堆符号
    title = title.replace('_', '').replace('-', '').replace(' ', '').replace('+', '')
    
    if not title:
        title = "未命名作业"
        
    return title

def main():
    name_to_id, id_to_name = get_student_map(SOURCE_MARKDOWN)
    print(f"加载了 {len(name_to_id)} 名学生信息")
    
    files = scan_files(SOURCE_FILES_DIR)
    print(f"扫描到 {len(files)} 个文件")
    
    count = 0
    for file_path, filename in files:
        # 1. 识别学生
        matched_name = None
        for name in name_to_id.keys():
            if name in filename:
                matched_name = name
                break
        
        # 特殊处理：如果是刘家名文件夹下的，且文件名里没名字
        if not matched_name and "刘家名" in file_path:
            matched_name = "刘家名"
            
        if not matched_name:
            print(f"[SKIP] 无法识别学生: {filename}")
            continue
            
        student_id = name_to_id[matched_name]
        
        # 2. 提取/生成日期
        date_str = extract_date(filename)
        if not date_str:
            # 使用文件修改时间
            mtime = os.path.getmtime(file_path)
            dt = datetime.fromtimestamp(mtime)
            date_str = dt.strftime("%Y%m%d")
            
        # 3. 提取题目
        # 简单处理：移除姓名，剩下的就是题目（做一些清理）
        # 或者直接保留原文件名作为题目的一部分，但为了规范化：姓名学号_题目_日期
        
        # 策略：提取关键信息作为 '题目'
        # 如果文件名包含 "论文", "作业", "游戏" 等关键词，可以尝试提取上下文
        # 这里为了通用，使用去重法
        
        title_part = clean_filename_parts(filename, matched_name, date_str)
        # 截断过长的 title
        if len(title_part) > 30:
            title_part = title_part[:30]
            
        ext = os.path.splitext(filename)[1]
        new_filename = f"{matched_name}{student_id}_{title_part}_{date_str}{ext}"
        
        # 4. 移动
        dest_dir = os.path.join(DEST_UPLOADS_DIR, student_id)
        if not os.path.exists(dest_dir):
            os.makedirs(dest_dir)
            
        dest_path = os.path.join(dest_dir, new_filename)
        
        # 检查是否已存在，如果存在则自动重命名 (添加 _1, _2...)
        base, extension = os.path.splitext(new_filename)
        counter = 1
        while os.path.exists(dest_path):
            new_filename = f"{base}_{counter}{extension}"
            dest_path = os.path.join(dest_dir, new_filename)
            counter += 1
            
        if counter > 1:
            print(f"[RENAME] 目标冲突，重命名为: {new_filename}")
            
        print(f"[MOVE] {filename} -> {student_id}/{new_filename}")
        shutil.move(file_path, dest_path)
        count += 1
        
    print(f"整理完成！共处理 {count} 个文件")

if __name__ == '__main__':
    main()
