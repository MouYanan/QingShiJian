const STORAGE_KEYS = {
    BILLS: 'bookkeepingData',
    TODOS: 'todoData',
    NOTES: 'noteData',
    BIRTHDAYS: 'birthdayData',
    THEME: 'themeSettings',
    NOTIFICATIONS: 'notificationSettings'
};

// 初始化应用主题
function initTheme() {
    const themeSettings = JSON.parse(localStorage.getItem(STORAGE_KEYS.THEME) || '{}');
    
    // 应用主题模式
    applyThemeMode(themeSettings.mode || 'light');
    
    // 应用背景图
    applyBackground(themeSettings);
}

// 应用主题模式（深色/浅色）
function applyThemeMode(mode) {
    // 移除之前的主题类
    document.body.classList.remove('light-theme', 'dark-theme');
    
    // 添加新主题类
    document.body.classList.add(`${mode}-theme`);
    
    // 更新主题设置
    const themeSettings = JSON.parse(localStorage.getItem(STORAGE_KEYS.THEME) || '{}');
    themeSettings.mode = mode;
    localStorage.setItem(STORAGE_KEYS.THEME, JSON.stringify(themeSettings));
}

// 应用背景图
function applyBackground(settings) {
    if (settings.background) {
        document.body.style.backgroundImage = `url('${settings.background}')`;
        document.body.style.backgroundSize = 'cover';
        document.body.style.backgroundPosition = 'center';
        document.body.style.backgroundAttachment = 'fixed';
        
        if (settings.blur) {
            document.body.style.backdropFilter = 'blur(5px)';
        } else {
            document.body.style.backdropFilter = 'none';
        }
    } else {
        document.body.style.backgroundImage = 'none';
        document.body.style.backdropFilter = 'none';
    }
}

// 在DOM加载完成后自动应用主题
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
});

function formatDate(dateStr) {
    if (!dateStr) return '';
    const [year, month, day] = dateStr.split('-');
    return `${year}年${parseInt(month)}月${parseInt(day)}日`;
}

function formatDateForInput(dateStr) {
    return dateStr || new Date().toISOString().split('T')[0];
}

function getPriorityText(priority) {
    const texts = {
        high: '高优先级',
        medium: '中优先级',
        low: '低优先级'
    };
    return texts[priority] || priority;
}

function getStatusText(status) {
    const texts = {
        pending: '未完成',
        completed: '已完成'
    };
    return texts[status] || status;
}

function getDaysUntilBirthday(birthdayStr) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const birthday = new Date(birthdayStr);
    birthday.setFullYear(today.getFullYear());
    
    if (birthday < today) {
        birthday.setFullYear(today.getFullYear() + 1);
    }
    
    const diffTime = birthday - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    return diffDays;
}

function showSuccessMessage(message = '保存成功！') {
    let successEl = document.getElementById('successMessage');
    if (!successEl) {
        successEl = document.createElement('div');
        successEl.id = 'successMessage';
        successEl.className = 'success-message';
        document.body.appendChild(successEl);
    }
    
    successEl.textContent = message;
    successEl.classList.add('show');
    
    setTimeout(() => {
        successEl.classList.remove('show');
    }, 3000);
}

function showConfirmDialog(message, onConfirm) {
    if (confirm(message)) {
        onConfirm();
    }
}

function navigateTo(url) {
    window.location.href = url;
}

function getUrlParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

function setUrlParam(param, value) {
    const url = new URL(window.location.href);
    url.searchParams.set(param, value);
    window.history.pushState({}, '', url);
}

function getFromStorage(key) {
    try {
        return JSON.parse(localStorage.getItem(key)) || [];
    } catch {
        return [];
    }
}

function saveToStorage(key, data) {
    localStorage.setItem(key, JSON.stringify(data));
}

function getBills() {
    return getFromStorage(STORAGE_KEYS.BILLS);
}

function saveBills(bills) {
    saveToStorage(STORAGE_KEYS.BILLS, bills);
}

function getTodos() {
    return getFromStorage(STORAGE_KEYS.TODOS);
}

function saveTodos(todos) {
    saveToStorage(STORAGE_KEYS.TODOS, todos);
}

function getNotes() {
    return getFromStorage(STORAGE_KEYS.NOTES);
}

function saveNotes(notes) {
    saveToStorage(STORAGE_KEYS.NOTES, notes);
}

function getBirthdays() {
    return getFromStorage(STORAGE_KEYS.BIRTHDAYS);
}

function saveBirthdays(birthdays) {
    saveToStorage(STORAGE_KEYS.BIRTHDAYS, birthdays);
}

function calculateTotalAmount(amountInputs) {
    let total = 0;
    amountInputs.forEach(input => {
        const value = parseFloat(input.value) || 0;
        total += value;
    });
    return total.toFixed(2);
}

function createCalendarReminder(title, description, date) {
    alert(`已创建日程提醒：\n标题：${title}\n内容：${description}\n日期：${formatDate(date)}`);
}

function formatDateForDisplay(dateStr) {
    if (!dateStr) return '未设置';
    return formatDate(dateStr);
}

function getPageTitle(pageName) {
    const titles = {
        'bill': '我的账单',
        'todo': '待办事项',
        'note': '笔记',
        'birthday': '生日管家',
        'settings': '设置'
    };
    return titles[pageName] || pageName;
}

function isToday(dateStr) {
    const today = new Date().toISOString().split('T')[0];
    return dateStr === today;
}

function isExpired(dateStr) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const date = new Date(dateStr);
    return date < today;
}

function getRelativeTime(dateStr) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const date = new Date(dateStr);
    const diffTime = date - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return '今天';
    if (diffDays === 1) return '明天';
    if (diffDays === -1) return '昨天';
    if (diffDays > 0) return `${diffDays}天后`;
    return `${Math.abs(diffDays)}天前`;
}

function initCircleButton(options) {
    const circleBtn = document.getElementById('circleBtn');
    const optionsContainer = document.getElementById('circleOptions');
    
    if (!circleBtn || !optionsContainer) return;
    
    let isExpanded = false;
    
    circleBtn.addEventListener('click', () => {
        isExpanded = !isExpanded;
        
        if (isExpanded) {
            circleBtn.style.transform = 'rotate(45deg)';
            renderCircleOptions(options);
        } else {
            circleBtn.style.transform = 'rotate(0deg)';
            optionsContainer.innerHTML = '';
        }
    });
}

function renderCircleOptions(options) {
    const container = document.getElementById('circleOptions');
    if (!container) return;
    
    container.innerHTML = '';
    
    options.forEach((option, index) => {
        const optionEl = document.createElement('div');
        optionEl.className = `circle-option ${option.color || 'blue'}`;
        optionEl.innerHTML = `<i class="${option.icon}"></i><span>${option.text}</span>`;
        optionEl.addEventListener('click', option.action);
        container.appendChild(optionEl);
    });
}

function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

function validateForm(formData, rules) {
    const errors = [];
    
    rules.forEach(rule => {
        const value = formData[rule.field];
        
        if (rule.required && !value) {
            errors.push(rule.message);
            return;
        }
        
        if (rule.minLength && value && value.length < rule.minLength) {
            errors.push(rule.message || `长度不能少于${rule.minLength}个字符`);
        }
        
        if (rule.pattern && value && !rule.pattern.test(value)) {
            errors.push(rule.message || '格式不正确');
        }
    });
    
    return errors;
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showSuccessMessage('已复制到剪贴板');
    }).catch(() => {
        console.error('复制失败');
    });
}

function exportData(data, filename) {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

function importData(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = JSON.parse(e.target.result);
                resolve(data);
            } catch {
                reject(new Error('文件格式不正确'));
            }
        };
        reader.onerror = () => reject(new Error('读取文件失败'));
        reader.readAsText(file);
    });
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function formatCurrency(amount) {
    return `¥${parseFloat(amount).toFixed(2)}`;
}

function getInitials(name) {
    if (!name) return '?';
    return name.charAt(0).toUpperCase();
}

function groupByDate(items, dateField = 'date') {
    return items.reduce((groups, item) => {
        const date = item[dateField];
        if (!groups[date]) {
            groups[date] = [];
        }
        groups[date].push(item);
        return groups;
    }, {});
}

function sortByDate(items, dateField = 'date', descending = true) {
    return [...items].sort((a, b) => {
        const dateA = new Date(a[dateField]);
        const dateB = new Date(b[dateField]);
        return descending ? dateB - dateA : dateA - dateB;
    });
}

function getCurrentPage() {
    const path = window.location.pathname;
    const filename = path.split('/').pop();
    return filename.replace('.html', '');
}

function setActiveNavItem(itemId) {
    document.querySelectorAll('.bottom-box a').forEach(link => {
        link.classList.remove('active');
    });
    const activeLink = document.querySelector(`.bottom-box a[href*="${itemId}"]`);
    if (activeLink) {
        activeLink.classList.add('active');
    }
}
