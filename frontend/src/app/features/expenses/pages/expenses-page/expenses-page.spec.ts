import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ExpensesService } from '../../services/expenses.service';
import { ExpensesPage } from './expenses-page';

describe('ExpensesPage', () => {
  it('shows the heading and the expense list', async () => {
    await TestBed.configureTestingModule({
      imports: [ExpensesPage],
      providers: [
        {
          provide: ExpensesService,
          useValue: {
            getTypes: () => of([]),
            search: () => of({ content: [], totalAmount: 0 }),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ExpensesPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Gastos');
    expect(fixture.nativeElement.querySelector('app-expense-list')).not.toBeNull();
  });
});
