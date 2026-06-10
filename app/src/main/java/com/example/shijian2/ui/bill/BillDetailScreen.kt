package com.example.shijian2.ui.bill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.shijian2.data.Bill
import com.example.shijian2.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    bill: Bill,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (bill is com.example.shijian2.data.ProjectBill) "项目名称" else "账单日期",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (bill is com.example.shijian2.data.ProjectBill) bill.name else if (bill is com.example.shijian2.data.TimeBill) DateUtil.formatDateForDisplay(bill.date) else "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("总金额", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥%.2f".format(bill.total),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF10b981)
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("支出明细", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    bill.expenses.forEach { expense ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (bill is com.example.shijian2.data.ProjectBill && expense.date != null) {
                                Column {
                                    Text(
                                        text = DateUtil.formatDateForDisplay(expense.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(expense.item, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Text(expense.item, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = "¥%.2f".format(expense.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("返回")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("修改账单")
                }
            }
        }
    }
