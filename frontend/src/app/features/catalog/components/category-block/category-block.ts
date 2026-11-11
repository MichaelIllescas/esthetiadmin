import { Icon } from '../../../../shared/components/icon/icon';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { formatPrice } from '../../../../shared/utils/format';
import { Category, Offering } from '../../models/catalog.models';

let nextId = 0;

/** A category with its services. It only shows things and reports what the user asked for. */
@Component({
  selector: 'app-category-block',
  imports: [Icon],
  templateUrl: './category-block.html',
  styleUrl: './category-block.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryBlock {
  readonly category = input.required<Category>();
  readonly showInactive = input(false);
  readonly collapsed = input(false);

  readonly toggleCollapse = output<void>();
  readonly edit = output<void>();
  readonly toggleActive = output<void>();
  readonly delete = output<void>();
  readonly addOffering = output<void>();
  readonly editOffering = output<Offering>();
  readonly toggleOfferingActive = output<Offering>();
  readonly deleteOffering = output<Offering>();

  protected readonly bodyId = `category-body-${nextId++}`;
  protected readonly offerings = computed(() => {
    const services = this.category().services;
    return this.showInactive() ? services : services.filter((service) => service.active);
  });

  protected readonly formatPrice = formatPrice;
}
