import os
import glob

layout_dir = r'c:\Users\MADAGADHA\AndroidStudioProjects\secondaplication\app\src\main\res\layout'
files = glob.glob(os.path.join(layout_dir, 'activity_*.xml'))

for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
        
    if 'android:fitsSystemWindows="true"' not in content:
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if '<' in line and not line.strip().startswith('<?'):
                parts = line.split(' ', 1)
                if len(parts) > 1:
                    lines[i] = parts[0] + ' android:fitsSystemWindows="true" ' + parts[1]
                else:
                    lines[i] = parts[0] + ' android:fitsSystemWindows="true"'
                break
        
        new_content = '\n'.join(lines)
        with open(f, 'w', encoding='utf-8') as file:
            file.write(new_content)
            print(f'Updated {f}')
